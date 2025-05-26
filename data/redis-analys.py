import re
from datetime import datetime
import pandas as pd
from collections import defaultdict, OrderedDict

class RedisLogAnalyzer:
    def __init__(self, log_file_path):
        self.log_file_path = log_file_path
        self.member_events = defaultdict(list)
        self.lock_events = []
        
    def parse_log_line(self, line):
        """로그 라인을 파싱하여 시간, 멤버ID, 이벤트 타입을 추출"""
        # 시간 패턴: 2025-05-26T05:12:07.514Z
        time_pattern = r'(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z)'
        
        # 멤버ID 패턴: memberId: 숫자
        member_pattern = r'memberId:\s*(\d+)'
        
        # 이벤트 타입 패턴들
        event_patterns = {
            'api_start': r'redis api start',
            'history_check': r'쿠폰 발급 내역 조회',
            'lock_attempt': r'redis lock 획득 시도',
            'lock_acquired': r'redis lock 획득(?!\s*시도)',
            'coupon_lookup': r'coupon 조회',
            'coupon_decrease': r'coupon 갯수 감소',
            'member_coupon_save': r'memberCoupon 저장 시도',
            'issue_complete': r'쿠폰 발급 완료',
            'lock_release': r'redis unlock'
        }
        
        time_match = re.search(time_pattern, line)
        member_match = re.search(member_pattern, line)
        
        if not time_match or not member_match:
            return None
            
        timestamp = datetime.fromisoformat(time_match.group(1).replace('Z', '+00:00'))
        member_id = int(member_match.group(1))
        
        # 이벤트 타입 확인
        event_type = None
        for event_name, pattern in event_patterns.items():
            if re.search(pattern, line):
                event_type = event_name
                break
                
        if event_type:
            return {
                'timestamp': timestamp,
                'member_id': member_id,
                'event_type': event_type,
                'raw_line': line.strip()
            }
        return None
    
    def load_and_parse_logs(self):
        """로그 파일을 읽고 파싱"""
        with open(self.log_file_path, 'r', encoding='utf-8') as f:
            for line in f:
                parsed = self.parse_log_line(line)
                if parsed:
                    self.member_events[parsed['member_id']].append(parsed)
                    
                    # lock 관련 이벤트는 별도 저장
                    if parsed['event_type'] in ['lock_acquired', 'lock_release']:
                        self.lock_events.append(parsed)
    
    def analyze_member_processing_time(self):
        """각 멤버별 처리 시간 분석"""
        results = []
        
        for member_id, events in self.member_events.items():
            # 이벤트를 시간순으로 정렬
            events.sort(key=lambda x: x['timestamp'])
            
            # 각 단계별 시간 추출
            event_times = {}
            for event in events:
                event_times[event['event_type']] = event['timestamp']
            
            if 'api_start' not in event_times or 'lock_release' not in event_times:
                continue
                
            # 전체 처리 시간
            total_time = (event_times['lock_release'] - event_times['api_start']).total_seconds() * 1000
            
            # 각 단계별 시간 계산
            stage_times = {}
            
            # 1. API 시작 → Lock 획득 시도
            if 'lock_attempt' in event_times:
                stage_times['wait_for_lock_attempt'] = (event_times['lock_attempt'] - event_times['api_start']).total_seconds() * 1000
            
            # 2. Lock 획득 시도 → Lock 실제 획득
            if 'lock_attempt' in event_times and 'lock_acquired' in event_times:
                stage_times['lock_wait_time'] = (event_times['lock_acquired'] - event_times['lock_attempt']).total_seconds() * 1000
            
            # 3. Lock 획득 → 쿠폰 발급 완료
            if 'lock_acquired' in event_times and 'issue_complete' in event_times:
                stage_times['processing_time'] = (event_times['issue_complete'] - event_times['lock_acquired']).total_seconds() * 1000
            
            # 4. 쿠폰 발급 완료 → Lock 해제
            if 'issue_complete' in event_times and 'lock_release' in event_times:
                stage_times['cleanup_time'] = (event_times['lock_release'] - event_times['issue_complete']).total_seconds() * 1000
            
            results.append({
                'member_id': member_id,
                'total_time_ms': round(total_time, 2),
                'wait_for_lock_attempt_ms': round(stage_times.get('wait_for_lock_attempt', 0), 2),
                'lock_wait_time_ms': round(stage_times.get('lock_wait_time', 0), 2),
                'processing_time_ms': round(stage_times.get('processing_time', 0), 2),
                'cleanup_time_ms': round(stage_times.get('cleanup_time', 0), 2),
                'start_time': event_times['api_start'],
                'end_time': event_times['lock_release']
            })
        
        return sorted(results, key=lambda x: x['start_time'])
    
    def analyze_lock_transitions(self):
        """Lock 전환 시간 분석 (한 멤버의 unlock과 다음 멤버의 lock 획득 사이 시간)"""
        # lock 이벤트를 시간순으로 정렬
        lock_events = sorted(self.lock_events, key=lambda x: x['timestamp'])
        
        transitions = []
        previous_release = None
        
        for event in lock_events:
            if event['event_type'] == 'lock_release':
                previous_release = event
            elif event['event_type'] == 'lock_acquired' and previous_release:
                # 이전 unlock과 현재 lock 획득 사이 시간
                transition_time = (event['timestamp'] - previous_release['timestamp']).total_seconds() * 1000
                transitions.append({
                    'from_member': previous_release['member_id'],
                    'to_member': event['member_id'],
                    'transition_time_ms': round(transition_time, 2),
                    'unlock_time': previous_release['timestamp'],
                    'lock_time': event['timestamp']
                })
        
        return transitions
    
    def generate_detailed_report(self):
        """상세 분석 리포트 생성"""
        print("=" * 80)
        print("Redis 쿠폰 발급 로그 분석 리포트")
        print("=" * 80)
        
        # 1. 멤버별 처리 시간 분석
        member_results = self.analyze_member_processing_time()
        
        print("\n1. 멤버별 처리 시간 분석")
        print("-" * 50)
        df_members = pd.DataFrame(member_results)
        print(f"총 처리된 멤버 수: {len(member_results)}")
        print(f"평균 전체 처리 시간: {df_members['total_time_ms'].mean():.2f}ms")
        print(f"최소/최대 처리 시간: {df_members['total_time_ms'].min():.2f}ms / {df_members['total_time_ms'].max():.2f}ms")
        
        # 상위 10개 멤버 상세 정보
        print("\n상위 10개 멤버 상세 처리 시간:")
        print(df_members[['member_id', 'total_time_ms', 'wait_for_lock_attempt_ms', 
                         'lock_wait_time_ms', 'processing_time_ms', 'cleanup_time_ms']].head(10).to_string(index=False))
        
        # 2. 단계별 평균 시간
        print("\n2. 단계별 평균 시간 분석")
        print("-" * 50)
        stage_avg = {
            '대기 시간 (API → Lock 시도)': df_members['wait_for_lock_attempt_ms'].mean(),
            'Lock 대기 시간': df_members['lock_wait_time_ms'].mean(),
            '실제 처리 시간': df_members['processing_time_ms'].mean(),
            '정리 시간': df_members['cleanup_time_ms'].mean()
        }
        
        for stage, avg_time in stage_avg.items():
            print(f"{stage}: {avg_time:.2f}ms")
        
        # 3. Lock 전환 분석
        transitions = self.analyze_lock_transitions()
        print(f"\n3. Lock 전환 시간 분석")
        print("-" * 50)
        
        if transitions:
            df_transitions = pd.DataFrame(transitions)
            print(f"총 Lock 전환 횟수: {len(transitions)}")
            print(f"평균 전환 시간: {df_transitions['transition_time_ms'].mean():.2f}ms")
            print(f"최소/최대 전환 시간: {df_transitions['transition_time_ms'].min():.2f}ms / {df_transitions['transition_time_ms'].max():.2f}ms")
            
            print("\n전환 시간이 긴 상위 5개:")
            print(df_transitions.nlargest(5, 'transition_time_ms')[['from_member', 'to_member', 'transition_time_ms']].to_string(index=False))
        
        # 4. 시간별 처리량 분석
        print(f"\n4. 시간별 처리 패턴")
        print("-" * 50)
        
        # 처리 시작 시간을 초 단위로 그룹화
        df_members['start_second'] = df_members['start_time'].dt.strftime('%H:%M:%S')
        processing_by_second = df_members.groupby('start_second').size()
        
        print("초당 처리 요청 수 (상위 5개):")
        print(processing_by_second.nlargest(5).to_string())
        
        return {
            'member_analysis': member_results,
            'lock_transitions': transitions,
            'summary_stats': {
                'total_members': len(member_results),
                'avg_total_time': df_members['total_time_ms'].mean(),
                'avg_lock_wait': df_members['lock_wait_time_ms'].mean(),
                'avg_processing': df_members['processing_time_ms'].mean(),
                'stage_averages': stage_avg
            }
        }

def analyze_redis_logs(log_content):
    """로그 내용을 직접 분석하는 함수"""
    import tempfile
    import os
    
    # 임시 파일에 로그 내용 저장
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.log', encoding='utf-8') as f:
        f.write(log_content)
        temp_file_path = f.name
    
    try:
        # 분석 수행
        analyzer = RedisLogAnalyzer(temp_file_path)
        analyzer.load_and_parse_logs()
        results = analyzer.generate_detailed_report()
        return results
    finally:
        # 임시 파일 삭제
        os.unlink(temp_file_path)

# 사용 예시
if __name__ == "__main__":
    # 파일에서 직접 읽기
    log_file_path = "redis.json"  # 로그 파일 경로
    
    analyzer = RedisLogAnalyzer(log_file_path)
    analyzer.load_and_parse_logs()
    results = analyzer.generate_detailed_report()
    
    # 결과를 CSV로 저장 (선택사항)
    member_df = pd.DataFrame(results['member_analysis'])
    member_df.to_csv('member_processing_analysis.csv', index=False)
    
    if results['lock_transitions']:
        transition_df = pd.DataFrame(results['lock_transitions'])
        transition_df.to_csv('lock_transition_analysis.csv', index=False)
    
    print("\n분석 완료! 결과가 CSV 파일로 저장되었습니다.")