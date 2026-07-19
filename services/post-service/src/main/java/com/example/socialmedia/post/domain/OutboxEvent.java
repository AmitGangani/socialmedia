package com.example.socialmedia.post.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_ATTEMPTS = 10;
    private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(60);

    @Id
    @Column(name = "event_id")
    @Getter
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    @Getter
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    @Getter
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    @Getter
    private int schemaVersion;

    @Column(nullable = false, length = 128)
    @Getter
    private String topic;

    @Column(name = "message_key", nullable = false, length = 128)
    @Getter
    private String messageKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    @Getter
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    @Getter
    private Instant occurredAt;

    @Column(name = "correlation_id", nullable = false, length = 128)
    @Getter
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OutboxEvent(UUID eventId, UUID aggregateId, String eventType, String topic,
            String messageKey, String payload, Instant occurredAt, String correlationId) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.schemaVersion = 1;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.status = Status.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = occurredAt;
        this.createdAt = occurredAt;
    }

    public void markPublished(Instant acknowledgedAt) {
        status = Status.PUBLISHED;
        publishedAt = acknowledgedAt;
        lastError = null;
    }

    public void recordFailure(Instant failedAt, String sanitizedError) {
        attemptCount++;
        lastError = sanitizedError.substring(0, Math.min(sanitizedError.length(), 1000));
        if (attemptCount >= MAX_ATTEMPTS) {
            status = Status.FAILED;
            nextAttemptAt = failedAt;
            return;
        }
        long delaySeconds = Math.min(1L << Math.min(attemptCount - 1, 6),
                MAX_RETRY_DELAY.toSeconds());
        nextAttemptAt = failedAt.plusSeconds(delaySeconds);
    }

    public enum Status {
        PENDING,
        PUBLISHED,
        FAILED
    }
}
