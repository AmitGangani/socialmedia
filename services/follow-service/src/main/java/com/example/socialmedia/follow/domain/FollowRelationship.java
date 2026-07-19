package com.example.socialmedia.follow.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "follow_relationship")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FollowRelationship {

    @Id
    @Getter
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    @Getter
    private UUID followerId;

    @Column(name = "followed_id", nullable = false)
    @Getter
    private UUID followedId;

    @Column(name = "followed_at", nullable = false, updatable = false)
    @Getter
    private Instant followedAt;

    public FollowRelationship(UUID id, UUID followerId, UUID followedId, Instant followedAt) {
        this.id = id;
        this.followerId = followerId;
        this.followedId = followedId;
        this.followedAt = followedAt;
    }
}
