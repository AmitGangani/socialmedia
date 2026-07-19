package com.example.socialmedia.user.domain;

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
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @Getter
    private UUID id;

    @Column(nullable = false, length = 254)
    @Getter
    private String email;

    @Column(name = "normalized_email", nullable = false, length = 254, unique = true)
    private String normalizedEmail;

    @Column(nullable = false, length = 30)
    @Getter
    private String username;

    @Column(name = "normalized_username", nullable = false, length = 30, unique = true)
    private String normalizedUsername;

    @Column(name = "display_name", nullable = false, length = 80)
    @Getter
    private String displayName;

    @Column(nullable = false, length = 160)
    @Getter
    private String bio;

    @Column(name = "password_hash", nullable = false, length = 100)
    @Getter
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Getter
    private Instant createdAt;

    public Account(UUID id, String email, String normalizedEmail, String username,
            String normalizedUsername, String displayName, String bio, String passwordHash,
            Instant createdAt) {
        this.id = id;
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.username = username;
        this.normalizedUsername = normalizedUsername;
        this.displayName = displayName;
        this.bio = bio;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }
}
