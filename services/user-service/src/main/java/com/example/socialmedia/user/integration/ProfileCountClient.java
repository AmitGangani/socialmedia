package com.example.socialmedia.user.integration;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

public class ProfileCountClient {

    private final RestClient followClient;
    private final RestClient postClient;

    public ProfileCountClient(RestClient.Builder builder, String followServiceBaseUrl,
            String postServiceBaseUrl) {
        this.followClient = builder.clone().baseUrl(followServiceBaseUrl).build();
        this.postClient = builder.clone().baseUrl(postServiceBaseUrl).build();
    }

    public ProfileCounts getCounts(UUID userId) {
        try {
            FollowCounts followCounts = followClient.get()
                    .uri("/internal/v1/users/{userId}/follow-counts", userId)
                    .retrieve()
                    .body(FollowCounts.class);
            PostCount postCount = postClient.get()
                    .uri("/internal/v1/users/{userId}/post-count", userId)
                    .retrieve()
                    .body(PostCount.class);
            if (followCounts == null || postCount == null) {
                throw unavailable();
            }
            return new ProfileCounts(followCounts.followerCount(), followCounts.followingCount(),
                    postCount.postCount());
        }
        catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Profile counts are temporarily unavailable");
    }

    private record FollowCounts(long followerCount, long followingCount) {
    }

    private record PostCount(long postCount) {
    }

    public record ProfileCounts(long followerCount, long followingCount, long postCount) {
    }
}
