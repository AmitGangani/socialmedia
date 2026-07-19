package com.example.socialmedia.post.api;

import java.util.UUID;

import com.example.socialmedia.post.persistence.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostRepository postRepository;

    @GetMapping("/internal/v1/users/{userId}/post-count")
    PostCount postCount(@PathVariable UUID userId) {
        return new PostCount(postRepository.countByAuthorIdAndDeletedAtIsNull(userId));
    }

    record PostCount(long postCount) {
    }
}
