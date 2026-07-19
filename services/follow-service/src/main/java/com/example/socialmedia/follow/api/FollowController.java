package com.example.socialmedia.follow.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.socialmedia.follow.application.FollowService;
import com.example.socialmedia.follow.config.CorrelationIdFilter;
import com.example.socialmedia.follow.persistence.FollowRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final FollowRepository followRepository;

    @PutMapping("/api/v1/follows/{followedUserId}")
    ResponseEntity<FollowView> follow(@PathVariable UUID followedUserId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(CorrelationIdFilter.HEADER_NAME) String correlationId) {
        FollowService.FollowResult result = followService.follow(subject(jwt), followedUserId,
                correlationId);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(FollowView.from(result));
    }

    @DeleteMapping("/api/v1/follows/{followedUserId}")
    ResponseEntity<Void> unfollow(@PathVariable UUID followedUserId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(CorrelationIdFilter.HEADER_NAME) String correlationId) {
        followService.unfollow(subject(jwt), followedUserId, correlationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/v1/users/{userId}/follow-counts")
    FollowCounts followCounts(@PathVariable UUID userId) {
        return new FollowCounts(followRepository.countByFollowedId(userId),
                followRepository.countByFollowerId(userId));
    }

    @GetMapping("/internal/v1/users/{userId}/followers")
    FollowerPage eligibleFollowers(@PathVariable UUID userId,
            @RequestParam Instant eligibleAt, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        FollowService.FollowerPage page = followService.eligibleFollowers(userId, eligibleAt,
                cursor, size);
        return new FollowerPage(page.items(), page.nextCursor());
    }

    private static UUID subject(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    record FollowView(UUID id, UUID followerId, UUID followedId, Instant followedAt) {
        private static FollowView from(FollowService.FollowResult result) {
            return new FollowView(result.id(), result.followerId(), result.followedId(),
                    result.followedAt());
        }
    }

    record FollowCounts(long followerCount, long followingCount) {
    }

    record FollowerPage(List<UUID> items, String nextCursor) {
    }
}
