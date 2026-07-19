package com.example.socialmedia.follow.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.socialmedia.follow.domain.FollowRelationship;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<FollowRelationship, UUID> {

    long countByFollowedId(UUID followedId);

    long countByFollowerId(UUID followerId);

    Optional<FollowRelationship> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    List<FollowRelationship> findByFollowedIdAndFollowedAtLessThanEqualOrderByFollowedAtAscFollowerIdAsc(
            UUID followedId, Instant eligibleAt, Pageable pageable);

    @Query("""
            select relationship from FollowRelationship relationship
            where relationship.followedId = :followedId
              and relationship.followedAt <= :eligibleAt
              and (relationship.followedAt > :cursorTime
                   or (relationship.followedAt = :cursorTime
                       and relationship.followerId > :cursorFollowerId))
            order by relationship.followedAt asc, relationship.followerId asc
            """)
    List<FollowRelationship> findEligiblePageAfter(@Param("followedId") UUID followedId,
            @Param("eligibleAt") Instant eligibleAt, @Param("cursorTime") Instant cursorTime,
            @Param("cursorFollowerId") UUID cursorFollowerId, Pageable pageable);
}
