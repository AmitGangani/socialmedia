package com.example.socialmedia.follow.integration;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.socialmedia.follow.domain.OutboxEvent;
import com.example.socialmedia.follow.persistence.OutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final Duration PUBLISHED_RETENTION = Duration.ofDays(7);
    private static final Duration FAILED_RETENTION = Duration.ofDays(30);
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${follow.outbox.poll-interval:PT1S}")
    @Transactional
    public void publishDueEvents() {
        Instant now = clock.instant();
        List<OutboxEvent> events = outboxRepository
                .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING, now, PageRequest.of(0, 100));
        for (OutboxEvent event : events) {
            MDC.put("correlationId", event.getCorrelationId());
            MDC.put("operation", "outbox.publish");
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(event.getTopic(), null,
                        null, event.getMessageKey(), envelope(event), headers(event));
                kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
                event.markPublished(clock.instant());
                LOG.info("Published eventId={} eventType={}", event.getEventId(), event.getEventType());
            }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                event.recordFailure(clock.instant(), sanitized(exception));
                LOG.warn("Outbox publish interrupted eventId={} eventType={}",
                        event.getEventId(), event.getEventType());
                return;
            }
            catch (Exception exception) {
                event.recordFailure(clock.instant(), sanitized(exception));
                LOG.warn("Outbox publish failed eventId={} eventType={}",
                        event.getEventId(), event.getEventType());
            }
            finally {
                MDC.remove("operation");
                MDC.remove("correlationId");
            }
        }
    }

    @Scheduled(fixedDelayString = "${follow.outbox.cleanup-interval:PT1H}", initialDelayString = "PT1M")
    @Transactional
    public void cleanupRetainedEvents() {
        Instant now = clock.instant();
        outboxRepository.deletePublishedBefore(now.minus(PUBLISHED_RETENTION));
        outboxRepository.deleteFailedBefore(now.minus(FAILED_RETENTION));
    }

    private String envelope(OutboxEvent event) throws Exception {
        JsonNode payload = objectMapper.readTree(event.getPayload());
        var envelope = objectMapper.createObjectNode();
        envelope.put("eventId", event.getEventId().toString());
        envelope.put("eventType", event.getEventType());
        envelope.put("schemaVersion", event.getSchemaVersion());
        envelope.put("aggregateId", event.getAggregateId().toString());
        envelope.put("occurredAt", event.getOccurredAt().toString());
        envelope.put("producer", "follow-service");
        envelope.put("correlationId", event.getCorrelationId());
        envelope.set("payload", payload);
        return objectMapper.writeValueAsString(envelope);
    }

    private static RecordHeaders headers(OutboxEvent event) {
        RecordHeaders headers = new RecordHeaders();
        addHeader(headers, "eventId", event.getEventId().toString());
        addHeader(headers, "eventType", event.getEventType());
        addHeader(headers, "correlationId", event.getCorrelationId());
        return headers;
    }

    private static void addHeader(RecordHeaders headers, String name, String value) {
        headers.add(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sanitized(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return "Kafka publish failed: " + cause.getClass().getSimpleName();
    }
}
