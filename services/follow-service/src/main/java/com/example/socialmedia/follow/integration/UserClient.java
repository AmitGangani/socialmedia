package com.example.socialmedia.follow.integration;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.clone().baseUrl(baseUrl).build();
    }

    public boolean exists(UUID userId, String correlationId) {
        try {
            ExistsResult result = restClient.get()
                    .uri("/internal/v1/users/{userId}/exists", userId)
                    .header("X-Correlation-Id", correlationId)
                    .retrieve()
                    .body(ExistsResult.class);
            if (result == null) {
                throw unavailable();
            }
            return result.exists();
        }
        catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "User validation is temporarily unavailable");
    }

    private record ExistsResult(boolean exists) {
    }
}
