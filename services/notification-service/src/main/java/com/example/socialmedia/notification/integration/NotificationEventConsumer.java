package com.example.socialmedia.notification.integration;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.example.socialmedia.notification.application.NotificationService;
import com.example.socialmedia.notification.domain.Notification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationEventConsumer.class);
    private static final Set<String> ENVELOPE_FIELDS = Set.of(
            "eventId", "eventType", "schemaVersion", "aggregateId", "occurredAt",
            "producer", "correlationId", "payload");
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = {"post-events.v1", "follow-events.v1"},
            groupId = NotificationService.CONSUMER_NAME)
    public void consume(ConsumerRecord<String, String> record) throws Exception {
        JsonNode envelope = objectMapper.readTree(record.value());
        validateEnvelope(envelope);
        UUID eventId = uuid(envelope, "eventId");
        String eventType = requiredText(envelope, "eventType");
        String correlationId = requiredText(envelope, "correlationId");
        JsonNode payload = envelope.path("payload");
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "notification.consume." + eventType);
        try {
            NotificationService.NotificationCandidate candidate = switch (eventType) {
                case "follow.created.v1" -> followCandidate(envelope, payload);
                case "post.published.v1" -> replyCandidate(envelope, payload);
                case "follow.removed.v1" -> {
                    validateFollowRemoved(envelope, payload);
                    yield null;
                }
                case "post.deleted.v1" -> {
                    validatePostDeleted(envelope, payload);
                    yield null;
                }
                default -> throw new IllegalArgumentException(
                        "Unknown event type: " + eventType);
            };
            NotificationService.ProcessResult result =
                    notificationService.process(eventId, eventType, candidate);
            LOG.info("Notification event processed eventId={} eventType={} outcome={}",
                    eventId, eventType, result);
        }
        catch (Exception exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            LOG.warn("Notification event failed eventId={} eventType={} failureType={} causeType={}",
                    eventId, eventType, exception.getClass().getSimpleName(),
                    cause.getClass().getSimpleName());
            throw exception;
        }
        finally {
            MDC.remove("operation");
            MDC.remove("correlationId");
        }
    }

    private static NotificationService.NotificationCandidate followCandidate(
            JsonNode envelope, JsonNode payload) {
        requireFields(payload, Set.of(
                "relationshipId", "followerId", "followedId", "followedAt"));
        UUID relationshipId = uuid(payload, "relationshipId");
        requireAggregate(envelope, relationshipId);
        return new NotificationService.NotificationCandidate(Notification.Type.FOLLOW,
                uuid(payload, "followedId"), uuid(payload, "followerId"), relationshipId,
                null, instant(payload, "followedAt"));
    }

    private static NotificationService.NotificationCandidate replyCandidate(
            JsonNode envelope, JsonNode payload) {
        requireFields(payload, Set.of(
                "postId", "authorId", "publishedAt", "parentPostId", "parentAuthorId"));
        UUID postId = uuid(payload, "postId");
        requireAggregate(envelope, postId);
        JsonNode parentPostId = payload.get("parentPostId");
        JsonNode parentAuthorId = payload.get("parentAuthorId");
        if (parentPostId.isNull() && parentAuthorId.isNull()) {
            return null;
        }
        if (!parentPostId.isTextual() || !parentAuthorId.isTextual()) {
            throw new IllegalArgumentException(
                    "Reply event parent fields must both be null or UUID strings");
        }
        return new NotificationService.NotificationCandidate(Notification.Type.REPLY,
                UUID.fromString(parentAuthorId.textValue()), uuid(payload, "authorId"), postId,
                UUID.fromString(parentPostId.textValue()), instant(payload, "publishedAt"));
    }

    private static void validateFollowRemoved(JsonNode envelope, JsonNode payload) {
        requireFields(payload, Set.of(
                "relationshipId", "followerId", "followedId", "unfollowedAt"));
        requireAggregate(envelope, uuid(payload, "relationshipId"));
        uuid(payload, "followerId");
        uuid(payload, "followedId");
        instant(payload, "unfollowedAt");
    }

    private static void validatePostDeleted(JsonNode envelope, JsonNode payload) {
        requireFields(payload, Set.of("postId", "authorId", "deletedAt"));
        requireAggregate(envelope, uuid(payload, "postId"));
        uuid(payload, "authorId");
        instant(payload, "deletedAt");
    }

    private static void validateEnvelope(JsonNode envelope) {
        requireFields(envelope, ENVELOPE_FIELDS);
        uuid(envelope, "eventId");
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

    private static void requireAggregate(JsonNode envelope, UUID payloadAggregateId) {
        if (!uuid(envelope, "aggregateId").equals(payloadAggregateId)) {
            throw new IllegalArgumentException(
                    "Event aggregate ID does not match its payload");
        }
    }

    private static void requireFields(JsonNode node, Set<String> fields) {
        if (node == null || !node.isObject() || node.size() != fields.size()) {
            throw new IllegalArgumentException("Event object has an invalid field shape");
        }
        node.fieldNames().forEachRemaining(name -> {
            if (!fields.contains(name)) {
                throw new IllegalArgumentException("Unknown event field: " + name);
            }
        });
        for (String field : fields) {
            if (!node.has(field)) {
                throw new IllegalArgumentException("Missing event field: " + field);
            }
        }
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
