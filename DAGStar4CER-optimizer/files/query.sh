curl \
  --user "elastic:elastic123" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -o "query_out.json" \
  -XGET 'http://192.168.1.100:9200/raw_stats/_search?pretty' -d @query_in.json