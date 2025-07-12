import json
import random

import requests
import sys
from time import sleep


def get_records_out(op, records_in):
    if op == 'duplicate':
        return tuple(map(lambda x: x * 2, records_in['duplicate']))
    elif op == 'map':
        return records_in['map']
    elif op == 'select':
        return records_in['select']
    else:
        raise RuntimeError('get_records_out else')


def periodic_tuples_to_logstash(LOGSTASH_URL, num_of_jobs):
    # Field choices
    JobID = [["job_{0}".format(x)] for x in range(5)]
    operator = ["filter", "join", "aggregate", "duplicate", "retrieve", "select", "map"]
    throughput_MB = {"filter": (5, 6), "join": (3, 4), "aggregate": (2.5, 3.5), "duplicate": (6, 8), "retrieve": (4, 6),
                     "select": (8, 9), "map": (5, 7.5)}
    records_in = {"filter": (1000, 1500), "join": (400, 1000), "aggregate": (600, 800), "duplicate": (100, 200),
                  "retrieve": (0, 0), "select": (1000, 1500), "map": (1000, 1500)}
    records_out = {"filter": (500, 1000), "join": (300, 500), "aggregate": (200, 450), "duplicate": ('USE records_in'),
                   "retrieve": (1500, 1600), "select": ('USE records_in'), "map": ('USE records_in')}
    op_latency = {"filter": (5, 6), "join": (20, 30), "aggregate": (20, 50), "duplicate": (9, 10),
                  "retrieve": (30, 40), "select": (4, 5), "map": (5, 8)}
    job_idx = 0
    for _ in range(num_of_jobs):
        job_name = JobID[job_idx]
        job_idx += 1
        job_idx %= len(JobID)
        lat = 0
        for op in operator:
            rec_out = records_out[op] if op not in ['select', 'duplicate', 'map'] else get_records_out(op, records_in)
            d_i = dict([
                ('JobID', job_name),
                ('operator', op),
                ('throughput', random.randint(throughput_MB[op][0] * (10 ** 6), throughput_MB[op][1] * (10 ** 6))),
                ('records_in', random.randint(records_in[op][0], records_in[op][1])),
                ('records_out', random.randint(rec_out[0], rec_out[1])),
                ('op_latency', round(random.uniform(op_latency[op][0], op_latency[op][1]), 3)),
                ('parallelism', 6)
            ])
            lat += d_i['op_latency']
            print(json.dumps(d_i))
            r = requests.post("http://{0}".format(LOGSTASH_URL), data=json.dumps(d_i), timeout=5)
            if r.status_code != 200:
                print('Status code of POST: '.format(r.status_code))
                break
        print('Latency: {0}'.format(lat), end='\n\n')


if __name__ == '__main__':
    ES_URL = sys.argv[1]
    LOGSTASH_URL = sys.argv[2]

    file_path = "tb.csv"
    fieldnames = ["filter", "join", "aggregate", "duplicate", "retrieve", "select", "map"]
    # Send tuples to Logstash periodically
    while True:
        try:
            periodic_tuples_to_logstash(LOGSTASH_URL, num_of_jobs=10)
            sleep(5)
        except KeyboardInterrupt as e:
            print('Will now exit.')
            exit(1)
