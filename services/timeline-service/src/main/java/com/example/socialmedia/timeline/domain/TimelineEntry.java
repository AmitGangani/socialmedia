package com.example.socialmedia.timeline.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("timeline_entry")
@Getter
public class TimelineEntry {

    @Id
    private final UUID id;

    @Column("owner_user_id")
    private final UUID ownerUserId;

    @Column("post_id")
    private final UUID postId;

    @Column("author_id")
    private final UUID authorId;

    @Column("published_at")
    private final Instant publishedAt;

    @Column("source_event_id")
    private final UUID sourceEventId;

    @Column("created_at")
    private final Instant createdAt;

    public TimelineEntry(UUID id, UUID ownerUserId, UUID postId, UUID authorId,
            Instant publishedAt, UUID sourceEventId, Instant createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.postId = postId;
        this.authorId = authorId;
        this.publishedAt = publishedAt;
        this.sourceEventId = sourceEventId;
        this.createdAt = createdAt;
    }
}
