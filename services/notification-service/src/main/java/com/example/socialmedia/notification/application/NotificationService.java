package com.example.socialmedia.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.example.socialmedia.notification.domain.Notification;
import com.example.socialmedia.notification.persistence.NotificationRepository;
import com.example.socialmedia.notification.persistence.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String CONSUMER_NAME = "notification-service-v1";
    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final CursorCodec cursorCodec;
    private final Supplier<UUID> uuidV7Generator;
    private final Clock clock;

    @Transactional
    public ProcessResult process(UUID eventId, String eventType, NotificationCandidate candidate) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Instant committedAt = now();
        if (processedEventRepository.insertIfAbsent(eventId, CONSUMER_NAME, eventType,
                committedAt) == 0) {
            return ProcessResult.DUPLICATE;
        }
        if (candidate == null || candidate.actorUserId().equals(candidate.recipientUserId())) {
            return ProcessResult.NO_VISIBLE_NOTIFICATION;
        }
        notificationRepository.save(new Notification(uuidV7Generator.get(),
                candidate.recipientUserId(), candidate.actorUserId(), candidate.type(),
                candidate.subjectId(), candidate.parentPostId(), candidate.eventTime(),
                eventId, committedAt));
        return ProcessResult.CREATED;
    }

    @Transactional(readOnly = true)
    public NotificationPage list(UUID recipientUserId, String encodedCursor, int size) {
        cursorCodec.validatePageSize(size);
        CursorCodec.Cursor cursor = cursorCodec.decode(encodedCursor);
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<Notification> rows = cursor == null
                ? notificationRepository
                        .findByRecipientUserIdOrderByEventTimeDescIdDesc(
                                recipientUserId, pageRequest)
                : notificationRepository.findRecipientPageAfter(recipientUserId,
                        cursor.eventTime(), cursor.id(), pageRequest);
        boolean hasMore = rows.size() > size;
        List<NotificationView> items = rows.stream().limit(size)
                .map(NotificationService::projection)
                .toList();
        String nextCursor = hasMore
                ? cursorCodec.encode(items.getLast().eventTime(), items.getLast().id()) : null;
        return new NotificationPage(items, nextCursor);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static NotificationView projection(Notification notification) {
        return new NotificationView(notification.getId(), notification.getType(),
                notification.getActorUserId(), notification.getSubjectId(),
                notification.getParentPostId(), notification.getEventTime(),
                notification.getAvailableAt());
    }

    public record NotificationCandidate(Notification.Type type, UUID recipientUserId,
            UUID actorUserId, UUID subjectId, UUID parentPostId, Instant eventTime) {

        public NotificationCandidate {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(recipientUserId, "recipientUserId");
            Objects.requireNonNull(actorUserId, "actorUserId");
            Objects.requireNonNull(subjectId, "subjectId");
            Objects.requireNonNull(eventTime, "eventTime");
            if (type == Notification.Type.FOLLOW && parentPostId != null) {
                throw new IllegalArgumentException(
                        "A follow notification candidate cannot have a parent post");
            }
            if (type == Notification.Type.REPLY && parentPostId == null) {
                throw new IllegalArgumentException(
                        "A reply notification candidate requires a parent post");
            }
        }
    }

    public record NotificationView(UUID id, Notification.Type type, UUID actorUserId,
            UUID subjectId, UUID parentPostId, Instant eventTime, Instant availableAt) {
    }

    public record NotificationPage(List<NotificationView> items, String nextCursor) {
    }

    public enum ProcessResult {
        CREATED,
        NO_VISIBLE_NOTIFICATION,
        DUPLICATE
    }
}
