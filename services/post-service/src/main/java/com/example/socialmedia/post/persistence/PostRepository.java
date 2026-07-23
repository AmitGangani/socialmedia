package com.example.socialmedia.post.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.socialmedia.post.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PostRepository extends JpaRepository<Post, UUID> {

    long countByAuthorIdAndDeletedAtIsNull(UUID authorId);

    Optional<Post> findByIdAndDeletedAtIsNull(UUID postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select post from Post post where post.id = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") UUID postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select post from Post post
            where post.id = :postId and post.deletedAt is null
            """)
    Optional<Post> findVisibleByIdForUpdate(@Param("postId") UUID postId);

    List<Post> findByAuthorIdAndDeletedAtIsNullOrderByPublishedAtDescIdDesc(
            UUID authorId, Pageable pageable);

    @Query("""
            select post from Post post
            where post.authorId = :authorId
              and post.deletedAt is null
              and (post.publishedAt < :sortTime
                   or (post.publishedAt = :sortTime and post.id < :cursorId))
            order by post.publishedAt desc, post.id desc
            """)
    List<Post> findVisibleAuthorPageAfter(@Param("authorId") UUID authorId,
            @Param("sortTime") Instant sortTime, @Param("cursorId") UUID cursorId,
            Pageable pageable);

    List<Post> findAllByIdInAndDeletedAtIsNull(Collection<UUID> postIds);

    @Query("""
            select post.id from Post post
            where post.id in :postIds and post.deletedAt is null
            """)
    List<UUID> findVisibleIds(@Param("postIds") Collection<UUID> postIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Post post set post.deletedAt = :deletedAt
            where post.id = :postId and post.authorId = :authorId and post.deletedAt is null
            """)
    int softDeleteVisible(@Param("postId") UUID postId, @Param("authorId") UUID authorId,
            @Param("deletedAt") Instant deletedAt);
}
