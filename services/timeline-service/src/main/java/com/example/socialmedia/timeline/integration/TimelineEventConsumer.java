package com.example.socialmedia.timeline.integration;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.example.socialmedia.timeline.application.TimelineService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class TimelineEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TimelineEventConsumer.class);
    private static final Set<String> ENVELOPE_FIELDS = Set.of(
            "eventId", "eventType", "schemaVersion", "aggregateId", "occurredAt",
            "producer", "correlationId", "payload");
    private final ObjectMapper objectMapper;
    private final TimelineService timelineService;

    @KafkaListener(topics = {"post-events.v1", "follow-events.v1"},
            groupId = "timeline-service-v1")
    public void consume(ConsumerRecord<String, String> record) throws Exception {
        JsonNode envelope = objectMapper.readTree(record.value());
        validateEnvelope(envelope);
        String eventType = requiredText(envelope, "eventType");
        String correlationId = requiredText(envelope, "correlationId");
        UUID eventId = UUID.fromString(requiredText(envelope, "eventId"));
        JsonNode payload = envelope.path("payload");
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "timeline.consume." + eventType);
        try {
            switch (eventType) {
                case "post.published.v1" -> LOG.info("Fan-out eventId={} inserted={}", eventId,
                        timelineService.applyPublishedPost(eventId, uuid(payload, "postId"),
                                uuid(payload, "authorId"), instant(payload, "publishedAt")));
                case "post.deleted.v1" -> timelineService.applyDeletedPost(uuid(payload, "postId"));
                case "follow.removed.v1" -> timelineService.applyRemovedFollowRelationship(
                        uuid(payload, "followerId"), uuid(payload, "followedId"),
                        instant(payload, "unfollowedAt"));
                case "follow.created.v1" -> LOG.debug("No historical timeline backfill eventId={}",
                        eventId);
                default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
            }
        }
        catch (Exception exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            LOG.warn("Timeline event failed eventId={} eventType={} failureType={} causeType={}",
                    eventId, eventType, exception.getClass().getSimpleName(),
                    cause.getClass().getSimpleName());
            throw exception;
        }
        finally {
            MDC.remove("operation");
            MDC.remove("correlationId");
        }
    }

    private static void validateEnvelope(JsonNode envelope) {
        requireFields(envelope, ENVELOPE_FIELDS);
        UUID aggregateId = uuid(envelope, "aggregateId");
        instant(envelope, "occurredAt");
        String eventType = requiredText(envelope, "eventType");
        String producer = requiredText(envelope, "producer");
        String expectedProducer = eventType.startsWith("post.") ? "post-service"
                : eventType.startsWith("follow.") ? "follow-service" : null;
        if (expectedProducer != null && !expectedProducer.equals(producer)) {
            throw new IllegalArgumentException("Event producer does not match event type");
        }
        JsonNode schemaVersion = envelope.path("schemaVersion");
        if (!schemaVersion.isInt() || schemaVersion.intValue() != 1) {
            throw new IllegalArgumentException("Unsupported event schema version");
        }
        String correlationId = requiredText(envelope, "correlationId");
        if (correlationId.length() > 128) {
            throw new IllegalArgumentException("Event correlation ID is too long");
        }
        if (!envelope.path("payload").isObject()) {
            throw new IllegalArgumentException("Event payload must be an object");
        }
        if (aggregateId.version() != 7) {
            throw new IllegalArgumentException("Aggregate ID must be UUIDv7");
        }
    }

    private static void requireFields(JsonNode node, Set<String> fields) {
        if (node == null || !node.isObject() || node.size() != fields.size()) {
            throw new IllegalArgumentException("Event object has an invalid field shape");
        }
        node.propertyNames().forEach(name -> {
            if (!fields.contains(name)) {
                throw new IllegalArgumentException("Unknown event field: " + name);
            }
        });
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Missing event field: " + field);
        }
        return value.textValue();
    }

    private static UUID uuid(JsonNode node, String field) {
        return UUID.fromString(requiredText(node, field));
    }

    private static Instant instant(JsonNode node, String field) {
        return Instant.parse(requiredText(node, field));
    }
}
