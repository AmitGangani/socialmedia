package com.example.socialmedia.timeline.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.socialmedia.timeline.domain.TimelineEntry;
import com.example.socialmedia.timeline.integration.FollowClient;
import com.example.socialmedia.timeline.integration.PostClient;
import com.example.socialmedia.timeline.persistence.TimelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private static final int FOLLOWER_PAGE_SIZE = 100;
    private final TimelineRepository timelineRepository;
    private final PostClient postClient;
    private final CursorCodec cursorCodec;
    private final FollowClient followClient;
    private final Clock clock;

    public int applyPublishedPost(UUID eventId, UUID postId, UUID authorId,
            Instant publishedAt) {
        int inserted = 0;
        String cursor = null;
        do {
            FollowClient.FollowerPage page = followClient.eligibleFollowers(authorId,
                    publishedAt, cursor, FOLLOWER_PAGE_SIZE);
            inserted += timelineRepository.insertReferences(page.items(), postId, authorId,
                    publishedAt, eventId, clock.instant());
            cursor = page.nextCursor();
        }
        while (cursor != null);
        return inserted;
    }

    public void applyDeletedPost(UUID postId) {
        timelineRepository.deleteByPostId(postId);
    }

    public void applyRemovedFollowRelationship(UUID followerId, UUID followedId,
            Instant unfollowedAt) {
        timelineRepository.deleteByOwnerAndAuthorThrough(followerId, followedId, unfollowedAt);
    }

    @Transactional(readOnly = true)
    public TimelinePage home(UUID ownerUserId, String encodedCursor, int size,
            String correlationId) {
        CursorCodec.Cursor cursor = cursorCodec.decode(encodedCursor);
        List<TimelineEntry> rows = timelineRepository.findOwnerPage(ownerUserId,
                cursor == null ? null : cursor.publishedAt(),
                cursor == null ? null : cursor.postId(), size + 1);
        boolean hasMore = rows.size() > size;
        List<TimelineEntry> references = rows.stream().limit(size).toList();
        if (references.isEmpty()) {
            return new TimelinePage(List.of(), null);
        }

        List<PostClient.PostSummary> hydrated = postClient.bulkVisible(
                references.stream().map(TimelineEntry::getPostId).toList(), correlationId);
        Map<UUID, PostClient.PostSummary> byId = hydrated.stream()
                .collect(Collectors.toMap(PostClient.PostSummary::id, Function.identity()));
        List<TimelineItem> items = references.stream()
                .map(reference -> item(byId.get(reference.getPostId()), ownerUserId))
                .filter(java.util.Objects::nonNull)
                .toList();
        TimelineEntry last = references.getLast();
        String nextCursor = hasMore
                ? cursorCodec.encode(last.getPublishedAt(), last.getPostId()) : null;
        return new TimelinePage(items, nextCursor);
    }

    private static TimelineItem item(PostClient.PostSummary post, UUID viewerId) {
        if (post == null) {
            return null;
        }
        return new TimelineItem(post.id(), post.authorId(), post.text(), post.publishedAt(),
                post.reply(), parentReference(post.parent()), post.likeCount(),
                viewerId.equals(post.authorId()));
    }

    private static ParentReference parentReference(PostClient.ParentReference parent) {
        return parent == null ? null : new ParentReference(parent.postId(), parent.available());
    }

    public record TimelineItem(UUID id, UUID authorId, String text, java.time.Instant publishedAt,
            boolean reply, ParentReference parent, long likeCount,
            boolean deletionAvailable) {
    }

    public record ParentReference(UUID postId, boolean available) {
    }

    public record TimelinePage(List<TimelineItem> items, String nextCursor) {
    }
}
