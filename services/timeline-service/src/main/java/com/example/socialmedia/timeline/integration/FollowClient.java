package com.example.socialmedia.timeline.integration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class FollowClient {

    private final RestClient restClient;

    public FollowClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.clone().baseUrl(baseUrl).build();
    }

    public FollowerPage eligibleFollowers(UUID followedId, Instant eligibleAt, String cursor,
            int size) {
        try {
            FollowerPage page = restClient.get()
                    .uri(builder -> builder.path("/internal/v1/users/{userId}/followers")
                            .queryParam("eligibleAt", eligibleAt)
                            .queryParamIfPresent("cursor", java.util.Optional.ofNullable(cursor))
                            .queryParam("size", size)
                            .build(followedId))
                    .retrieve()
                    .body(FollowerPage.class);
            if (page == null || page.items() == null) {
                throw new IllegalStateException("Follow returned an invalid follower page");
            }
            return page;
        }
        catch (RestClientException exception) {
            throw new IllegalStateException("Eligible followers are temporarily unavailable",
                    exception);
        }
    }

    public record FollowerPage(List<UUID> items, String nextCursor) {
    }
}
