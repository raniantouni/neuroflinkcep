ARG ELK_VERSION

# https://github.com/elastic/logstash-docker
FROM docker.elastic.co/logstash/logstash:${ELK_VERSION}

# Add your logstash plugins setup here

RUN logstash-plugin install logstash-filter-kv
RUN logstash-plugin install logstash-filter-json
