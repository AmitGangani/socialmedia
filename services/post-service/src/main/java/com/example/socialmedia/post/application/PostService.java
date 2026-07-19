package com.example.socialmedia.post.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.example.socialmedia.post.domain.OutboxEvent;
import com.example.socialmedia.post.domain.Post;
import com.example.socialmedia.post.persistence.OutboxRepository;
import com.example.socialmedia.post.persistence.PostRepository;
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
public class PostService {

    private static final String POST_TOPIC = "post-events.v1";
    private final PostRepository postRepository;
    private final OutboxRepository outboxRepository;
    private final CursorCodec cursorCodec;
    private final ObjectMapper objectMapper;
    private final Supplier<UUID> uuidV7Generator;
    private final Clock clock;

    @Transactional
    public PostResult createOriginal(UUID authorId, String text, String correlationId) {
        validateText(text);
        Instant publishedAt = now();
        Post post = new Post(uuidV7Generator.get(), authorId, text, publishedAt);
        postRepository.save(post);
        outboxRepository.save(new OutboxEvent(uuidV7Generator.get(), post.getId(),
                "post.published.v1", POST_TOPIC, post.getId().toString(),
                json(new PublishedPayload(post.getId(), authorId, publishedAt, null, null)),
                publishedAt, correlationId));
        return projection(post);
    }

    @Transactional(readOnly = true)
    public PostResult getVisible(UUID postId) {
        return postRepository.findByIdAndDeletedAtIsNull(postId)
                .map(PostService::projection)
                .orElseThrow(PostService::notFound);
    }

    @Transactional(readOnly = true)
    public PostPage listVisibleByAuthor(UUID authorId, String encodedCursor, int size) {
        CursorCodec.Cursor cursor = cursorCodec.decode(encodedCursor);
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<Post> rows = cursor == null
                ? postRepository.findByAuthorIdAndDeletedAtIsNullOrderByPublishedAtDescIdDesc(
                        authorId, pageRequest)
                : postRepository.findVisibleAuthorPageAfter(authorId, cursor.sortTime(),
                        cursor.id(), pageRequest);
        boolean hasMore = rows.size() > size;
        List<PostResult> items = rows.stream().limit(size).map(PostService::projection).toList();
        String nextCursor = hasMore
                ? cursorCodec.encode(items.getLast().publishedAt(), items.getLast().id()) : null;
        return new PostPage(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public List<PostResult> bulkVisible(List<UUID> postIds) {
        if (postIds == null || postIds.isEmpty() || postIds.size() > 100
                || new HashSet<>(postIds).size() != postIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "postIds must contain between 1 and 100 unique IDs");
        }
        return postRepository.findAllByIdInAndDeletedAtIsNull(postIds).stream()
                .map(PostService::projection)
                .toList();
    }

    @Transactional
    public void delete(UUID postId, UUID authorId, String correlationId) {
        Post existing = postRepository.findById(postId).orElseThrow(PostService::notFound);
        if (!existing.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the post author can delete this post");
        }
        if (existing.getDeletedAt() != null) {
            return;
        }

        Instant deletedAt = now();
        if (postRepository.softDeleteVisible(postId, authorId, deletedAt) == 0) {
            return;
        }
        outboxRepository.save(new OutboxEvent(uuidV7Generator.get(), postId,
                "post.deleted.v1", POST_TOPIC, postId.toString(),
                json(new DeletedPayload(postId, authorId, deletedAt)), deletedAt, correlationId));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize post outbox payload", exception);
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Post text must not be blank");
        }
        int codePoints = text.codePointCount(0, text.length());
        if (codePoints < 1 || codePoints > 280) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Post text must contain between 1 and 280 Unicode code points");
        }
    }

    private static PostResult projection(Post post) {
        return new PostResult(post.getId(), post.getAuthorId(), post.getText(),
                post.getPublishedAt(), post.getParentPostId());
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
    }

    private record PublishedPayload(UUID postId, UUID authorId, Instant publishedAt,
            UUID parentPostId, UUID parentAuthorId) {
    }

    private record DeletedPayload(UUID postId, UUID authorId, Instant deletedAt) {
    }

    public record PostResult(UUID id, UUID authorId, String text, Instant publishedAt,
            UUID parentPostId) {
    }

    public record PostPage(List<PostResult> items, String nextCursor) {
    }
}
