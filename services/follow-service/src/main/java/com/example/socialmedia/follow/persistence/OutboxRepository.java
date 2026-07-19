package com.example.socialmedia.follow.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.socialmedia.follow.domain.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEvent.Status status, Instant dueAt, Pageable pageable);

    @Modifying
    @Query("delete from OutboxEvent event where event.status = 'PUBLISHED' and event.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from OutboxEvent event where event.status = 'FAILED' and event.createdAt < :cutoff")
    int deleteFailedBefore(@Param("cutoff") Instant cutoff);
}
