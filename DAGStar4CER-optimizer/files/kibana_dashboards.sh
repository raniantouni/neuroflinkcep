#!/usr/bin/env bash

. .env
curl -X GET -u "elastic:elastic123" "192.168.1.100:5601/api/kibana/dashboards/export?dashboard=5867db00-f0f0-11ea-95c9-afe53b4b5d11" -H 'kbn-xsrf: true'

#curl -X POST -u "elastic:elastic123" "192.168.1.100:5601/api/kibana/dashboards/import?exclude=index-pattern" -H 'kbn-xsrf: true' -H 'Content-Type: application/json' -d@file.json