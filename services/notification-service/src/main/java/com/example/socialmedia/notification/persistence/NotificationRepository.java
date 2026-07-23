package com.example.socialmedia.notification.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.socialmedia.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findBySourceEventId(UUID sourceEventId);

    boolean existsBySourceEventId(UUID sourceEventId);

    List<Notification> findByRecipientUserIdOrderByEventTimeDescIdDesc(
            UUID recipientUserId, Pageable pageable);

    @Query("""
            select notification from Notification notification
            where notification.recipientUserId = :recipientUserId
              and (notification.eventTime < :eventTime
                   or (notification.eventTime = :eventTime and notification.id < :id))
            order by notification.eventTime desc, notification.id desc
            """)
    List<Notification> findRecipientPageAfter(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("eventTime") Instant eventTime,
            @Param("id") UUID id,
            Pageable pageable);
}
