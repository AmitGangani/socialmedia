package com.example.socialmedia.notification.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.example.socialmedia.notification.domain.Notification;
import com.example.socialmedia.notification.persistence.NotificationRepository;
import com.example.socialmedia.notification.persistence.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private CursorCodec cursorCodec;

    @Test
    void duplicateFactCreatesOneNotification() {
        UUID eventId = UUID.randomUUID();
        Instant eventTime = Instant.parse("2026-09-08T12:00:00Z");
        NotificationService service = new NotificationService(notificationRepository,
                processedEventRepository, cursorCodec, UUID::randomUUID,
                Clock.fixed(eventTime.plusSeconds(1), ZoneOffset.UTC));
        NotificationService.NotificationCandidate candidate =
                new NotificationService.NotificationCandidate(Notification.Type.FOLLOW,
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, eventTime);
        when(processedEventRepository.insertIfAbsent(eq(eventId),
                eq(NotificationService.CONSUMER_NAME), eq("follow.created.v1"), any()))
                .thenReturn(1, 0);

        assertEquals(NotificationService.ProcessResult.CREATED,
                service.process(eventId, "follow.created.v1", candidate));
        assertEquals(NotificationService.ProcessResult.DUPLICATE,
                service.process(eventId, "follow.created.v1", candidate));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
