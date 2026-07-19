package com.example.socialmedia.follow.api;

import java.util.UUID;

import com.example.socialmedia.follow.persistence.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowRepository followRepository;

    @GetMapping("/internal/v1/users/{userId}/follow-counts")
    FollowCounts followCounts(@PathVariable UUID userId) {
        return new FollowCounts(followRepository.countByFollowedId(userId),
                followRepository.countByFollowerId(userId));
    }

    record FollowCounts(long followerCount, long followingCount) {
    }
}
