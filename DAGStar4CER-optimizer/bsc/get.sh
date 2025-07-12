#!/usr/bin/env bash
curl -X GET -u elastic:elastic123 http://192.168.1.200:9200/bsc-ingestion/_search | jq
