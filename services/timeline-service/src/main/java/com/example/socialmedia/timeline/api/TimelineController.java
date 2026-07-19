package com.example.socialmedia.timeline.api;

import java.util.List;
import java.util.UUID;

import com.example.socialmedia.timeline.application.TimelineService;
import com.example.socialmedia.timeline.config.CorrelationIdFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/api/v1/timeline/home")
    TimelinePage home(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(CorrelationIdFilter.HEADER_NAME) String correlationId) {
        TimelineService.TimelinePage page = timelineService.home(
                UUID.fromString(jwt.getSubject()), cursor, size, correlationId);
        return new TimelinePage(page.items(), page.nextCursor());
    }

    record TimelinePage(List<TimelineService.TimelineItem> items, String nextCursor) {
    }
}
