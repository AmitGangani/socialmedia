package com.example.socialmedia.timeline.integration;

import java.time.Instant;
import java.util.UUID;

import com.example.socialmedia.timeline.application.TimelineService;
import com.example.socialmedia.timeline.persistence.TimelineRepository;
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
public class TimelineEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TimelineEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final TimelineService timelineService;
    private final TimelineRepository timelineRepository;

    @KafkaListener(topics = {"post-events.v1", "follow-events.v1"},
            groupId = "timeline-service-v1")
    public void consume(ConsumerRecord<String, String> record) throws Exception {
        JsonNode envelope = objectMapper.readTree(record.value());
        String eventType = requiredText(envelope, "eventType");
        String correlationId = requiredText(envelope, "correlationId");
        UUID eventId = UUID.fromString(requiredText(envelope, "eventId"));
        JsonNode payload = envelope.path("payload");
        if (!payload.isObject()) {
            throw new IllegalArgumentException("Event payload must be an object");
        }
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "timeline.consume." + eventType);
        try {
            switch (eventType) {
                case "post.published.v1" -> LOG.info("Fan-out eventId={} inserted={}", eventId,
                        timelineService.applyPublishedPost(eventId, uuid(payload, "postId"),
                                uuid(payload, "authorId"), instant(payload, "publishedAt")));
                case "post.deleted.v1" -> timelineRepository.deleteByPostId(uuid(payload, "postId"));
                case "follow.removed.v1" -> timelineRepository.deleteByOwnerAndAuthorThrough(
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
