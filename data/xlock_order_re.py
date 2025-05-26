import pandas as pd

def reorder_member_data_by_transition_order():
    """
    xlock_member_processing_analysis_old.csv의 행 순서를 
    xlock_transition_analysis_old.csv의 from_member 순서와 맞춰서 재정렬
    """
    
    # 1. 두 CSV 파일 읽기
    print("CSV 파일들을 읽는 중...")
    member_df = pd.read_csv('xlock_member_processing_analysis.csv')
    transition_df = pd.read_csv('xlock_transition_analysis.csv')
    
    # 2. 데이터 확인
    print(f"멤버 데이터: {len(member_df)} 행")
    print(f"전환 데이터: {len(transition_df)} 행")
    print(f"멤버 데이터의 member_id 범위: {member_df['member_id'].min()} ~ {member_df['member_id'].max()}")
    print(f"전환 데이터의 from_member 범위: {transition_df['from_member'].min()} ~ {transition_df['from_member'].max()}")
    
    # 3. transition 데이터에서 from_member 순서 추출
    from_member_order = transition_df['from_member'].tolist()
    print(f"\n전환 순서 (첫 10개): {from_member_order[:10]}")
    
    # 4. 재정렬 수행 - merge를 사용한 방법
    print("\n데이터 재정렬 중...")
    
    # from_member 순서로 DataFrame 생성
    order_df = pd.DataFrame({
        'member_id': from_member_order,
        'order': range(len(from_member_order))
    })
    
    # member_df와 order_df를 merge하여 순서 정보 추가
    merged_df = member_df.merge(order_df, on='member_id', how='inner')
    
    # order 컬럼으로 정렬
    reordered_df = merged_df.sort_values('order').drop('order', axis=1).reset_index(drop=True)
    
    print(f"재정렬 완료: {len(reordered_df)} 행")
    print(f"재정렬된 데이터의 member_id 순서 (첫 10개): {reordered_df['member_id'].head(10).tolist()}")
    
    # 5. 누락 데이터 확인
    missing_in_member = set(from_member_order) - set(member_df['member_id'])
    if missing_in_member:
        print(f"경고: 다음 member_id들이 멤버 데이터에서 누락됨: {missing_in_member}")
    
    missing_in_transition = set(member_df['member_id']) - set(from_member_order)
    if missing_in_transition:
        print(f"정보: 다음 member_id들은 전환 데이터에 없음: {missing_in_transition}")
    
    # 6. 새 파일로 저장
    output_filename = 'xlock_member_processing_analysis_reordered.csv'
    reordered_df.to_csv(output_filename, index=False)
    print(f"\n재정렬된 데이터가 '{output_filename}'로 저장되었습니다.")
    
    # 7. 검증: 순서가 맞는지 확인
    print("\n=== 순서 검증 ===")
    transition_order = transition_df['from_member'].tolist()
    reordered_order = reordered_df['member_id'].tolist()
    
    if transition_order == reordered_order:
        print("✅ 순서가 정확히 맞습니다!")
    else:
        print("❌ 순서가 일치하지 않습니다.")
        print("전환 순서 길이:", len(transition_order))
        print("재정렬 순서 길이:", len(reordered_order))
        
        # 처음 10개만 비교해서 출력
        min_len = min(len(transition_order), len(reordered_order))
        for i in range(min(10, min_len)):
            if transition_order[i] != reordered_order[i]:
                print(f"위치 {i}: 전환={transition_order[i]}, 재정렬={reordered_order[i]}")
    
    return reordered_df

def verify_reordering():
    """재정렬 결과 검증 함수"""
    print("\n=== 재정렬 결과 상세 검증 ===")
    
    try:
        # 파일들 읽기
        transition_df = pd.read_csv('xlock_transition_analysis_old.csv')
        reordered_df = pd.read_csv('xlock_member_processing_analysis_reordered.csv')
        
        # 순서 비교
        transition_order = transition_df['from_member'].tolist()
        reordered_order = reordered_df['member_id'].tolist()
        
        print(f"전환 데이터 행 수: {len(transition_order)}")
        print(f"재정렬 데이터 행 수: {len(reordered_order)}")
        
        # 완전 비교
        if transition_order == reordered_order:
            print("✅ 완벽하게 일치합니다!")
        else:
            print("❌ 불일치가 발견되었습니다.")
            
            # 상세 비교
            min_len = min(len(transition_order), len(reordered_order))
            mismatches = 0
            
            for i in range(min_len):
                if transition_order[i] != reordered_order[i]:
                    if mismatches < 5:  # 처음 5개 불일치만 출력
                        print(f"위치 {i}: 전환={transition_order[i]}, 재정렬={reordered_order[i]}")
                    mismatches += 1
            
            if mismatches > 5:
                print(f"... 총 {mismatches}개의 불일치 발견")
        
        # 처음 15개 순서 출력
        print(f"\n전환 순서 (처음 15개): {transition_order[:15]}")
        print(f"재정렬 순서 (처음 15개): {reordered_order[:15]}")
        
    except FileNotFoundError as e:
        print(f"파일을 찾을 수 없습니다: {e}")
    except Exception as e:
        print(f"검증 중 오류 발생: {e}")

def show_sample_data():
    """결과 데이터 샘플 출력"""
    try:
        reordered_df = pd.read_csv('xlock_member_processing_analysis_reordered.csv')
        print("\n=== 재정렬된 데이터 샘플 ===")
        print(reordered_df.head(10))
        print(f"\n컬럼 정보: {list(reordered_df.columns)}")
        print(f"데이터 타입:\n{reordered_df.dtypes}")
        
    except FileNotFoundError:
        print("재정렬된 파일이 아직 생성되지 않았습니다.")
    except Exception as e:
        print(f"데이터 출력 중 오류: {e}")

if __name__ == "__main__":
    # 메인 실행
    print("=== CSV 행 순서 재정렬 시작 ===")
    
    try:
        result_df = reorder_member_data_by_transition_order()
        
        if result_df is not None:
            # 검증 실행
            verify_reordering()
            
            # 샘플 데이터 출력
            show_sample_data()
            
            # 최종 요약
            print(f"\n=== 최종 결과 ===")
            print(f"✅ 재정렬 완료!")
            print(f"📁 출력 파일: xlock_member_processing_analysis_reordered.csv")
            print(f"📊 데이터 행 수: {len(result_df)}")
            print(f"📋 컬럼 수: {len(result_df.columns)}")
            
        else:
            print("❌ 재정렬에 실패했습니다.")
            
    except Exception as e:
        print(f"❌ 실행 중 오류 발생: {e}")
        import traceback
        traceback.print_exc()