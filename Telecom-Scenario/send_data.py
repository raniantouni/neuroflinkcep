from kafka import KafkaProducer
import pandas as pd
import json

producer = KafkaProducer(
    bootstrap_servers='172.19.207.64:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

with open('MobileFraudDetectionData.txt', 'r', encoding='utf-8') as f:
    data = []
    for index, line in enumerate(f):
        parts = line.strip().split(';')
        if index == 0:
            columns = parts[::2]
        else:
            data.append(parts[1::2])
df = pd.DataFrame(data, columns=columns)

topic_name = 'mobile-fraud-data'
for _, row in df.iterrows():
    message = row.to_dict()
    producer.send(topic_name, value=message)

producer.flush()
print(f"Sent {len(df)} messages to topic '{topic_name}'")
