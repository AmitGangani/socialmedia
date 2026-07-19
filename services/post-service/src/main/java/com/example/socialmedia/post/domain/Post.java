package com.example.socialmedia.post.domain;

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
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @Getter
    private UUID id;

    @Column(name = "author_id", nullable = false)
    @Getter
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "text")
    @Getter
    private String text;

    @Column(name = "published_at", nullable = false, updatable = false)
    @Getter
    private Instant publishedAt;

    @Column(name = "parent_post_id")
    @Getter
    private UUID parentPostId;

    @Column(name = "parent_author_id")
    @Getter
    private UUID parentAuthorId;

    @Column(name = "deleted_at")
    @Getter
    private Instant deletedAt;

    public Post(UUID id, UUID authorId, String text, Instant publishedAt) {
        this.id = id;
        this.authorId = authorId;
        this.text = text;
        this.publishedAt = publishedAt;
    }
}
