package com.example.socialmedia.timeline.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import com.example.socialmedia.timeline.application.TimelineService;
import com.github.f4b6a3.uuid.UuidCreator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TimelineEventConsumerTest {

    @Mock
    private TimelineService timelineService;

    @Test
    void deletedPostFactReachesHomeFeedModule() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID postId = UuidCreator.getTimeOrderedEpoch();
        TimelineEventConsumer consumer = new TimelineEventConsumer(
                new ObjectMapper(), timelineService);
        String value = """
                {"eventId":"%s","eventType":"post.deleted.v1","schemaVersion":1,
                 "aggregateId":"%s","occurredAt":"2026-09-08T12:00:00Z",
                 "producer":"post-service","correlationId":"correlation-1",
                 "payload":{"postId":"%s"}}
                """.formatted(eventId, postId, postId);

        consumer.consume(new ConsumerRecord<>("post-events.v1", 0, 0, postId.toString(), value));

        verify(timelineService).applyDeletedPost(postId);
    }

    @Test
    void unexpectedEnvelopeFieldIsRejected() {
        UUID eventId = UUID.randomUUID();
        UUID postId = UuidCreator.getTimeOrderedEpoch();
        TimelineEventConsumer consumer = new TimelineEventConsumer(
                new ObjectMapper(), timelineService);
        String value = """
                {"eventId":"%s","eventType":"post.deleted.v1","schemaVersion":1,
                 "aggregateId":"%s","occurredAt":"2026-09-08T12:00:00Z",
                 "producer":"post-service","correlationId":"correlation-1",
                 "unexpected":true,"payload":{"postId":"%s"}}
                """.formatted(eventId, postId, postId);

        assertThrows(IllegalArgumentException.class, () -> consumer.consume(
                new ConsumerRecord<>("post-events.v1", 0, 0, postId.toString(), value)));
        verifyNoInteractions(timelineService);
    }
}
