package com.example.socialmedia.notification.persistence;

import java.time.Instant;
import java.util.UUID;

import com.example.socialmedia.notification.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, ProcessedEvent.Key> {

    boolean existsByConsumerNameAndEventId(String consumerName, UUID eventId);

    @Modifying
    @Query(value = """
            insert into processed_event (event_id, consumer_name, event_type, processed_at)
            values (:eventId, :consumerName, :eventType, :processedAt)
            on conflict (consumer_name, event_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId,
            @Param("consumerName") String consumerName,
            @Param("eventType") String eventType,
            @Param("processedAt") Instant processedAt);
}
