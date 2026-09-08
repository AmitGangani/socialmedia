package com.example.socialmedia.post.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

import com.example.socialmedia.post.domain.OutboxEvent;
import com.example.socialmedia.post.persistence.OutboxRepository;
import com.example.socialmedia.post.persistence.PostLikeRepository;
import com.example.socialmedia.post.persistence.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private CursorCodec cursorCodec;

    @Test
    void creatingPostAlsoStoresPublishedFact() {
        UUID postId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-09-08T12:00:00Z");
        ArrayDeque<UUID> ids = new ArrayDeque<>(List.of(postId, eventId));
        PostService service = new PostService(postRepository, postLikeRepository,
                outboxRepository, cursorCodec, new ObjectMapper().findAndRegisterModules(),
                ids::removeFirst, Clock.fixed(publishedAt, ZoneOffset.UTC));

        PostService.PostResult result = service.createOriginal(authorId, "Hello", "correlation-1");

        ArgumentCaptor<OutboxEvent> fact = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(fact.capture());
        assertEquals(postId, result.id());
        assertEquals("post.published.v1", fact.getValue().getEventType());
        assertEquals(postId, fact.getValue().getAggregateId());
        assertTrue(fact.getValue().getPayload().contains(postId.toString()));
    }
}
