import json
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
import os

# === 실험 파일 설정 ===
base_dir = './results'  # 결과 JSON이 저장된 디렉토리 경로

test_cases = [
    ('redis', 50),
    ('xlock', 50),
]

# === 데이터 수집 ===
records = []

for method, vus in test_cases:
    file_path = os.path.join(base_dir, f'{method}_{vus}vus.json')
    with open(file_path) as f:
        data = [json.loads(line) for line in f if line.strip()]

    for entry in data:
        if entry.get("type") == "Point" and entry.get("metric") == "http_req_duration":
            try:
                ts = datetime.fromtimestamp(float(entry["data"]["time"]) / 1000)
            except (ValueError, TypeError):
                ts = datetime.fromisoformat(entry["data"]["time"])

            records.append({
                'method': method,
                'vus': vus,
                'duration': entry["data"]["value"],
                'timestamp': ts,
            })

# === DataFrame 변환 ===
df = pd.DataFrame(records)

# === 응답 시간 요약 통계 출력 ===
print("=== 응답 시간 요약 통계 (ms) ===")
print(df.groupby(['method', 'vus'])['duration'].describe(percentiles=[0.5, 0.95, 0.99]))

# === Boxplot: 락 방식 vs VU 수 별 응답 시간 분포 ===
plt.figure(figsize=(12, 6))
sns.boxplot(x='vus', y='duration', hue='method', data=df)
plt.title('Redis vs X-lock Response Time Distribution (by VU count)')
plt.xlabel('Number of Virtual Users (VU)')
plt.ylabel('Response Time (ms)')
plt.grid(True)
plt.tight_layout()
plt.show()

# === Lineplot: 응답 시간 평균 변화 ===
df_mean = df.groupby(['method', 'vus'])['duration'].mean().reset_index()

plt.figure(figsize=(10, 5))
sns.lineplot(data=df_mean, x='vus', y='duration', hue='method', marker='o')
plt.title('Average Response Time by Number of VUs')
plt.xlabel('Number of Virtual Users (VU)')
plt.ylabel('Average Response Time (ms)')
plt.grid(True)
plt.tight_layout()
plt.show()

# === Lineplot: 시간 흐름에 따른 응답 시간 ===
plt.figure(figsize=(12, 6))
sns.lineplot(data=df, x='timestamp', y='duration', hue='method')
plt.title('Response Time Trend Over Time')
plt.ylabel('Response Time (ms)')
plt.xlabel('Timestamp')
plt.tight_layout()
plt.grid(True)
plt.show()
