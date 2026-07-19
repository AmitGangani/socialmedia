package com.example.socialmedia.follow.persistence;

import java.util.UUID;

import com.example.socialmedia.follow.domain.FollowRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<FollowRelationship, UUID> {

    long countByFollowedId(UUID followedId);

    long countByFollowerId(UUID followerId);
}
