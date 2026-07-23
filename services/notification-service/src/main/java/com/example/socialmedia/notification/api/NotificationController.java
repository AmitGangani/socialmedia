package com.example.socialmedia.notification.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.socialmedia.notification.application.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/v1/notifications")
    NotificationPage notifications(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        NotificationService.NotificationPage page = notificationService.list(
                UUID.fromString(jwt.getSubject()), cursor, size);
        return new NotificationPage(page.items().stream()
                .map(item -> new NotificationView(item.id(), item.type().name(),
                        item.actorUserId(), item.subjectId(), item.parentPostId(),
                        item.eventTime(), item.availableAt()))
                .toList(), page.nextCursor());
    }

    record NotificationView(UUID id, String type, UUID actorUserId, UUID subjectId,
            UUID parentPostId, Instant eventTime, Instant availableAt) {
    }

    record NotificationPage(List<NotificationView> items, String nextCursor) {
    }
}
