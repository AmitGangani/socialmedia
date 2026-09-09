package com.example.socialmedia.timeline.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.example.socialmedia.timeline.integration.FollowClient;
import com.example.socialmedia.timeline.integration.PostClient;
import com.example.socialmedia.timeline.persistence.TimelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-08T12:00:00Z");
    private static final Instant APPLIED_AT = Instant.parse("2026-09-08T12:00:01Z");

    @Mock
    private TimelineRepository timelineRepository;
    @Mock
    private PostClient postClient;
    @Mock
    private CursorCodec cursorCodec;
    @Mock
    private FollowClient followClient;

    private TimelineService timelineService;

    @BeforeEach
    void setUp() {
        timelineService = new TimelineService(timelineRepository, postClient, cursorCodec,
                followClient, Clock.fixed(APPLIED_AT, ZoneOffset.UTC));
    }

    @Test
    void publishedPostCreatesFeedEntries() {
        UUID eventId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        List<UUID> firstPage = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<UUID> secondPage = List.of(UUID.randomUUID());
        when(followClient.eligibleFollowers(authorId, PUBLISHED_AT, null, 100))
                .thenReturn(new FollowClient.FollowerPage(firstPage, "next"));
        when(followClient.eligibleFollowers(authorId, PUBLISHED_AT, "next", 100))
                .thenReturn(new FollowClient.FollowerPage(secondPage, null));
        when(timelineRepository.insertReferences(firstPage, postId, authorId, PUBLISHED_AT,
                eventId, APPLIED_AT)).thenReturn(2);
        when(timelineRepository.insertReferences(secondPage, postId, authorId, PUBLISHED_AT,
                eventId, APPLIED_AT)).thenReturn(1);

        int inserted = timelineService.applyPublishedPost(eventId, postId, authorId, PUBLISHED_AT);

        assertEquals(3, inserted);
    }

    @Test
    void deletedPostRemovesFeedEntries() {
        UUID postId = UUID.randomUUID();

        timelineService.applyDeletedPost(postId);

        verify(timelineRepository).deleteByPostId(postId);
    }

    @Test
    void removedFollowRelationshipRemovesFeedEntries() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();

        timelineService.applyRemovedFollowRelationship(followerId, followedId, PUBLISHED_AT);

        verify(timelineRepository).deleteByOwnerAndAuthorThrough(
                followerId, followedId, PUBLISHED_AT);
    }
}
