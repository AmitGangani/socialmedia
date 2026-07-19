package com.example.socialmedia.post.persistence;

import java.util.UUID;

import com.example.socialmedia.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, UUID> {

    long countByAuthorIdAndDeletedAtIsNull(UUID authorId);
}
