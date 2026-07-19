package com.example.socialmedia.timeline.integration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

public class PostClient {

    private static final Logger LOG = LoggerFactory.getLogger(PostClient.class);
    private final RestClient restClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public PostClient(RestClient.Builder builder, String baseUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = builder.clone().baseUrl(baseUrl).build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public List<PostSummary> bulkVisible(List<UUID> postIds, String correlationId) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        String previousCorrelationId = MDC.get("correlationId");
        MDC.put("correlationId", correlationId);
        try {
            LOG.info("Hydrating timelinePageSize={} through one bulk Post call", postIds.size());
            return circuitBreakerFactory.create("timeline-post-bulk").run(() -> {
                BulkPostResponse response = restClient.post()
                        .uri("/internal/v1/posts/bulk")
                        .header("X-Correlation-Id", correlationId)
                        .body(new BulkPostRequest(postIds))
                        .retrieve()
                        .body(BulkPostResponse.class);
                if (response == null || response.items() == null) {
                    throw new IllegalStateException("Post returned an invalid bulk response");
                }
                return response.items();
            }, failure -> {
                throw unavailable();
            });
        }
        finally {
            if (previousCorrelationId == null) {
                MDC.remove("correlationId");
            }
            else {
                MDC.put("correlationId", previousCorrelationId);
            }
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Timeline posts are temporarily unavailable");
    }

    private record BulkPostRequest(List<UUID> postIds) {
    }

    private record BulkPostResponse(List<PostSummary> items) {
    }

    public record ParentReference(UUID postId, boolean available) {
    }

    public record PostSummary(UUID id, UUID authorId, String text, Instant publishedAt,
            boolean reply, ParentReference parent, long likeCount) {
    }
}
