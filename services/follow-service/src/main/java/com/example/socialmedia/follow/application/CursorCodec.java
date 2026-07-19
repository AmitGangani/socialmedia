package com.example.socialmedia.follow.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class CursorCodec {

    private static final Set<String> FIELDS = Set.of("v", "followedAt", "followerId");
    private final ObjectMapper objectMapper;

    public String encode(Instant followedAt, UUID followerId) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(
                    new CursorDocument(1, followedAt, followerId));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode follower cursor", exception);
        }
    }

    public Cursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(new String(
                    Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8));
            if (root == null || !root.isObject() || root.size() != FIELDS.size()) {
                throw malformed();
            }
            root.fieldNames().forEachRemaining(name -> {
                if (!FIELDS.contains(name)) {
                    throw malformed();
                }
            });
            if (!root.path("v").isInt() || root.path("v").intValue() != 1
                    || !root.path("followedAt").isTextual()
                    || !root.path("followerId").isTextual()) {
                throw malformed();
            }
            return new Cursor(Instant.parse(root.path("followedAt").textValue()),
                    UUID.fromString(root.path("followerId").textValue()));
        }
        catch (IllegalArgumentException | JsonProcessingException | DateTimeParseException exception) {
            throw malformed();
        }
    }

    private static ResponseStatusException malformed() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Malformed or unsupported follower cursor");
    }

    private record CursorDocument(int v, Instant followedAt, UUID followerId) {
    }

    public record Cursor(Instant followedAt, UUID followerId) {
    }
}
