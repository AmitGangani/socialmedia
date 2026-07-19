package com.example.socialmedia.post.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.socialmedia.post.application.PostService;
import com.example.socialmedia.post.config.CorrelationIdFilter;
import com.example.socialmedia.post.persistence.PostRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostRepository postRepository;

    @PostMapping("/api/v1/posts")
    ResponseEntity<PostView> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(CorrelationIdFilter.HEADER_NAME) String correlationId,
            @Valid @RequestBody CreatePostRequest request) {
        UUID authorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PostView.from(postService.createOriginal(authorId, request.text(),
                        correlationId), authorId));
    }

    @GetMapping("/api/v1/posts/{postId}")
    PostView get(@PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        return PostView.from(postService.getVisible(postId), subject(jwt));
    }

    @DeleteMapping("/api/v1/posts/{postId}")
    ResponseEntity<Void> delete(@PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(CorrelationIdFilter.HEADER_NAME) String correlationId) {
        postService.delete(postId, UUID.fromString(jwt.getSubject()), correlationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/users/{userId}/posts")
    PostPage listByAuthor(@PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = subject(jwt);
        PostService.PostPage page = postService.listVisibleByAuthor(userId, cursor, size);
        return new PostPage(page.items().stream()
                .map(post -> PostView.from(post, viewerId)).toList(), page.nextCursor());
    }

    @GetMapping("/internal/v1/users/{userId}/post-count")
    PostCount postCount(@PathVariable UUID userId) {
        return new PostCount(postRepository.countByAuthorIdAndDeletedAtIsNull(userId));
    }

    @PostMapping("/internal/v1/posts/bulk")
    BulkPostResponse bulk(@Valid @RequestBody BulkPostRequest request) {
        return new BulkPostResponse(postService.bulkVisible(request.postIds()).stream()
                .map(PostSummary::from).toList());
    }

    private static UUID subject(Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record CreatePostRequest(@NotNull String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record BulkPostRequest(@NotNull @Size(min = 1, max = 100)
            List<@NotNull UUID> postIds) {
    }

    record ParentReference(UUID postId, boolean available) {
    }

    record PostView(UUID id, UUID authorId, String text, Instant publishedAt, boolean reply,
            ParentReference parent, long likeCount, boolean deletionAvailable) {

        private static PostView from(PostService.PostResult post, UUID viewerId) {
            return new PostView(post.id(), post.authorId(), post.text(), post.publishedAt(),
                    post.parentPostId() != null, null, 0,
                    viewerId != null && viewerId.equals(post.authorId()));
        }
    }

    record PostSummary(UUID id, UUID authorId, String text, Instant publishedAt, boolean reply,
            ParentReference parent, long likeCount) {

        private static PostSummary from(PostService.PostResult post) {
            return new PostSummary(post.id(), post.authorId(), post.text(), post.publishedAt(),
                    post.parentPostId() != null, null, 0);
        }
    }

    record PostPage(List<PostView> items, String nextCursor) {
    }

    record PostCount(long postCount) {
    }

    record BulkPostResponse(List<PostSummary> items) {
    }
}
