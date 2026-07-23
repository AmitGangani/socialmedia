package com.example.socialmedia.post.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_like", uniqueConstraints = @UniqueConstraint(
        name = "post_like_user_unique", columnNames = {"post_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @Id
    @Getter
    private UUID id;

    @Column(name = "post_id", nullable = false, updatable = false)
    @Getter
    private UUID postId;

    @Column(name = "user_id", nullable = false, updatable = false)
    @Getter
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Getter
    private Instant createdAt;

    public PostLike(UUID id, UUID postId, UUID userId, Instant createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.createdAt = createdAt;
    }
}
