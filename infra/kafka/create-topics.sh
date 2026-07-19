#!/usr/bin/env sh
set -eu

bootstrap_servers="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
kafka_topics="${KAFKA_TOPICS_COMMAND:-/opt/kafka/bin/kafka-topics.sh}"

until "$kafka_topics" --bootstrap-server "$bootstrap_servers" --list >/dev/null 2>&1; do
    echo "Waiting for Kafka at ${bootstrap_servers}..."
    sleep 2
done

create_topic() {
    topic_name="$1"
    retention_ms="$2"

    "$kafka_topics" \
        --bootstrap-server "$bootstrap_servers" \
        --create \
        --if-not-exists \
        --topic "$topic_name" \
        --partitions 3 \
        --replication-factor 1 \
        --config cleanup.policy=delete \
        --config "retention.ms=${retention_ms}"
}

create_topic post-events.v1 604800000
create_topic follow-events.v1 604800000
create_topic post-events.v1.timeline-dlt 2592000000
create_topic follow-events.v1.timeline-dlt 2592000000
create_topic post-events.v1.notification-dlt 2592000000
create_topic follow-events.v1.notification-dlt 2592000000

"$kafka_topics" --bootstrap-server "$bootstrap_servers" --list
