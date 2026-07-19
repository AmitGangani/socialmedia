package com.example.socialmedia.post.application;

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

    private static final Set<String> FIELDS = Set.of("v", "sortTime", "id");
    private final ObjectMapper objectMapper;

    public String encode(Instant sortTime, UUID id) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(new CursorDocument(1, sortTime, id));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode post cursor", exception);
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
                    || !root.path("sortTime").isTextual() || !root.path("id").isTextual()) {
                throw malformed();
            }
            return new Cursor(Instant.parse(root.path("sortTime").textValue()),
                    UUID.fromString(root.path("id").textValue()));
        }
        catch (IllegalArgumentException | JsonProcessingException | DateTimeParseException exception) {
            throw malformed();
        }
    }

    private static ResponseStatusException malformed() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Malformed or unsupported post cursor");
    }

    private record CursorDocument(int v, Instant sortTime, UUID id) {
    }

    public record Cursor(Instant sortTime, UUID id) {
    }
}
