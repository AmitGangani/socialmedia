package com.example.socialmedia.notification.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_event")
@IdClass(ProcessedEvent.Key.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    @Getter
    private UUID eventId;

    @Id
    @Column(name = "consumer_name", nullable = false, length = 64, updatable = false)
    @Getter
    private String consumerName;

    @Column(name = "event_type", nullable = false, length = 64, updatable = false)
    @Getter
    private String eventType;

    @Column(name = "processed_at", nullable = false, updatable = false)
    @Getter
    private Instant processedAt;

    public ProcessedEvent(UUID eventId, String consumerName, String eventType,
            Instant processedAt) {
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static final class Key implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private UUID eventId;
        private String consumerName;
    }
}
