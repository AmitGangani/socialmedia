package com.example.socialmedia.post.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.example.socialmedia.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    long countByPostId(UUID postId);

    @Query("""
            select postLike.postId as postId, count(postLike.id) as likeCount
            from PostLike postLike
            where postLike.postId in :postIds
            group by postLike.postId
            """)
    List<PostLikeCount> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    @Modifying
    @Query(value = """
            insert into post_like (id, post_id, user_id, created_at)
            values (:id, :postId, :userId, :createdAt)
            on conflict (post_id, user_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("postId") UUID postId,
            @Param("userId") UUID userId, @Param("createdAt") Instant createdAt);

    interface PostLikeCount {

        UUID getPostId();

        long getLikeCount();
    }
}
