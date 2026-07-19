package com.example.socialmedia.follow.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.example.socialmedia.follow.domain.FollowRelationship;
import com.example.socialmedia.follow.domain.OutboxEvent;
import com.example.socialmedia.follow.integration.UserClient;
import com.example.socialmedia.follow.persistence.FollowRepository;
import com.example.socialmedia.follow.persistence.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FollowService {

    private static final String FOLLOW_TOPIC = "follow-events.v1";
    private final FollowRepository followRepository;
    private final OutboxRepository outboxRepository;
    private final UserClient userClient;
    private final CursorCodec cursorCodec;
    private final ObjectMapper objectMapper;
    private final Supplier<UUID> uuidV7Generator;
    private final Clock clock;

    @Transactional
    public FollowResult follow(UUID followerId, UUID followedId, String correlationId) {
        if (followerId.equals(followedId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A user cannot follow themselves");
        }
        if (!userClient.exists(followedId, correlationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found");
        }
        return followRepository.findByFollowerIdAndFollowedId(followerId, followedId)
                .map(existing -> projection(existing, false))
                .orElseGet(() -> create(followerId, followedId, correlationId));
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followedId, String correlationId) {
        FollowRelationship relationship = followRepository
                .findByFollowerIdAndFollowedId(followerId, followedId).orElse(null);
        if (relationship == null) {
            return;
        }
        Instant unfollowedAt = now();
        followRepository.delete(relationship);
        outboxRepository.save(new OutboxEvent(uuidV7Generator.get(), relationship.getId(),
                "follow.removed.v1", FOLLOW_TOPIC, messageKey(followerId, followedId),
                json(new RemovedPayload(relationship.getId(), followerId, followedId,
                        unfollowedAt)), unfollowedAt, correlationId));
    }

    @Transactional(readOnly = true)
    public FollowerPage eligibleFollowers(UUID followedId, Instant eligibleAt,
            String encodedCursor, int size) {
        CursorCodec.Cursor cursor = cursorCodec.decode(encodedCursor);
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<FollowRelationship> rows = cursor == null
                ? followRepository
                        .findByFollowedIdAndFollowedAtLessThanEqualOrderByFollowedAtAscFollowerIdAsc(
                                followedId, eligibleAt, pageRequest)
                : followRepository.findEligiblePageAfter(followedId, eligibleAt,
                        cursor.followedAt(), cursor.followerId(), pageRequest);
        boolean hasMore = rows.size() > size;
        List<FollowRelationship> page = rows.stream().limit(size).toList();
        String nextCursor = hasMore
                ? cursorCodec.encode(page.getLast().getFollowedAt(), page.getLast().getFollowerId())
                : null;
        return new FollowerPage(page.stream().map(FollowRelationship::getFollowerId).toList(),
                nextCursor);
    }

    private FollowResult create(UUID followerId, UUID followedId, String correlationId) {
        Instant followedAt = now();
        FollowRelationship relationship = new FollowRelationship(uuidV7Generator.get(),
                followerId, followedId, followedAt);
        followRepository.save(relationship);
        outboxRepository.save(new OutboxEvent(uuidV7Generator.get(), relationship.getId(),
                "follow.created.v1", FOLLOW_TOPIC, messageKey(followerId, followedId),
                json(new CreatedPayload(relationship.getId(), followerId, followedId, followedAt)),
                followedAt, correlationId));
        return projection(relationship, true);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize follow outbox payload", exception);
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static String messageKey(UUID followerId, UUID followedId) {
        return followerId + ":" + followedId;
    }

    private static FollowResult projection(FollowRelationship relationship, boolean created) {
        return new FollowResult(relationship.getId(), relationship.getFollowerId(),
                relationship.getFollowedId(), relationship.getFollowedAt(), created);
    }

    private record CreatedPayload(UUID relationshipId, UUID followerId, UUID followedId,
            Instant followedAt) {
    }

    private record RemovedPayload(UUID relationshipId, UUID followerId, UUID followedId,
            Instant unfollowedAt) {
    }

    public record FollowResult(UUID id, UUID followerId, UUID followedId, Instant followedAt,
            boolean created) {
    }

    public record FollowerPage(List<UUID> items, String nextCursor) {
    }
}
