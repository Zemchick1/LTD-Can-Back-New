FROM docker.elastic.co/logstash/logstash:8.11.1
WORKDIR /logstash

COPY bin/*.jar .

COPY cfg/*.conf .

CMD ["logstash", "-f", "logstash.conf"]