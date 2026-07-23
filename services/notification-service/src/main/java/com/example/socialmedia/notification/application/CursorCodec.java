package com.example.socialmedia.notification.application;

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

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> FIELDS = Set.of("v", "eventTime", "id");
    private final ObjectMapper objectMapper;

    public String encode(Instant eventTime, UUID id) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(
                    new CursorDocument(1, eventTime, id));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode notification cursor", exception);
        }
    }

    public Cursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            JsonNode root = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            if (root == null || !root.isObject() || root.size() != FIELDS.size()) {
                throw malformed();
            }
            root.fieldNames().forEachRemaining(name -> {
                if (!FIELDS.contains(name)) {
                    throw malformed();
                }
            });
            if (!root.path("v").isInt() || root.path("v").intValue() != 1
                    || !root.path("eventTime").isTextual()
                    || !root.path("id").isTextual()) {
                throw malformed();
            }
            return new Cursor(Instant.parse(root.path("eventTime").textValue()),
                    UUID.fromString(root.path("id").textValue()));
        }
        catch (IllegalArgumentException | JsonProcessingException | DateTimeParseException exception) {
            throw malformed();
        }
    }

    public void validatePageSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Notification page size must be between 1 and 100");
        }
    }

    private static ResponseStatusException malformed() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Malformed or unsupported notification cursor");
    }

    private record CursorDocument(int v, Instant eventTime, UUID id) {
    }

    public record Cursor(Instant eventTime, UUID id) {
    }
}
