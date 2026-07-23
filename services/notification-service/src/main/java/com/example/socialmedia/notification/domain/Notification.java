package com.example.socialmedia.notification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification",
        uniqueConstraints = @UniqueConstraint(name = "notification_source_event_unique",
                columnNames = "source_event_id"),
        indexes = @Index(name = "notification_recipient_keyset_idx",
                columnList = "recipient_user_id, event_time DESC, id DESC"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @Getter
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false, updatable = false)
    @Getter
    private UUID recipientUserId;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    @Getter
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    @Getter
    private Type type;

    @Column(name = "subject_id", nullable = false, updatable = false)
    @Getter
    private UUID subjectId;

    @Column(name = "parent_post_id", updatable = false)
    @Getter
    private UUID parentPostId;

    @Column(name = "event_time", nullable = false, updatable = false)
    @Getter
    private Instant eventTime;

    @Column(name = "source_event_id", nullable = false, updatable = false, unique = true)
    @Getter
    private UUID sourceEventId;

    @Column(name = "available_at", nullable = false, updatable = false)
    @Getter
    private Instant availableAt;

    public Notification(UUID id, UUID recipientUserId, UUID actorUserId, Type type,
            UUID subjectId, UUID parentPostId, Instant eventTime, UUID sourceEventId,
            Instant availableAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId");
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId");
        this.type = Objects.requireNonNull(type, "type");
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId");
        this.eventTime = Objects.requireNonNull(eventTime, "eventTime");
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "sourceEventId");
        this.availableAt = Objects.requireNonNull(availableAt, "availableAt");
        if (type == Type.FOLLOW && parentPostId != null) {
            throw new IllegalArgumentException("A follow notification cannot have a parent post");
        }
        if (type == Type.REPLY && parentPostId == null) {
            throw new IllegalArgumentException("A reply notification requires a parent post");
        }
        this.parentPostId = parentPostId;
    }

    public enum Type {
        FOLLOW,
        REPLY
    }
}
