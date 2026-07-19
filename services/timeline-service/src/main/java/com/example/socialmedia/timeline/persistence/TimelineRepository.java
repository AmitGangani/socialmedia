package com.example.socialmedia.timeline.persistence;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.example.socialmedia.timeline.domain.TimelineEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class TimelineRepository {

    private final JdbcClient jdbcClient;
    private final Supplier<UUID> uuidV7Generator;

    @Transactional
    public int insertReferences(List<UUID> ownerUserIds, UUID postId, UUID authorId,
            Instant publishedAt, UUID sourceEventId, Instant createdAt) {
        if (ownerUserIds.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("""
                INSERT INTO timeline_entry
                    (id, owner_user_id, post_id, author_id, published_at, source_event_id, created_at)
                VALUES
                """);
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (int index = 0; index < ownerUserIds.size(); index++) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append("(:id").append(index).append(", :owner").append(index)
                    .append(", :postId, :authorId, :publishedAt, :sourceEventId, :createdAt)");
            parameters.put("id" + index, uuidV7Generator.get());
            parameters.put("owner" + index, ownerUserIds.get(index));
        }
        sql.append(" ON CONFLICT (owner_user_id, post_id) DO NOTHING");
        return jdbcClient.sql(sql.toString())
                .params(parameters)
                .param("postId", postId)
                .param("authorId", authorId)
                .param("publishedAt", publishedAt.atOffset(ZoneOffset.UTC))
                .param("sourceEventId", sourceEventId)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .update();
    }

    @Transactional
    public int deleteByPostId(UUID postId) {
        return jdbcClient.sql("DELETE FROM timeline_entry WHERE post_id = :postId")
                .param("postId", postId).update();
    }

    @Transactional
    public int deleteByOwnerAndAuthorThrough(UUID ownerUserId, UUID authorId,
            Instant publishedThrough) {
        return jdbcClient.sql("""
                DELETE FROM timeline_entry
                WHERE owner_user_id = :ownerUserId
                  AND author_id = :authorId
                  AND published_at <= :publishedThrough
                """)
                .param("ownerUserId", ownerUserId)
                .param("authorId", authorId)
                .param("publishedThrough", publishedThrough.atOffset(ZoneOffset.UTC))
                .update();
    }

    @Transactional(readOnly = true)
    public List<TimelineEntry> findOwnerPage(UUID ownerUserId, Instant cursorTime,
            UUID cursorPostId, int limit) {
        JdbcClient.StatementSpec query;
        if (cursorTime == null) {
            query = jdbcClient.sql("""
                    SELECT id, owner_user_id, post_id, author_id, published_at,
                           source_event_id, created_at
                    FROM timeline_entry
                    WHERE owner_user_id = :ownerUserId
                    ORDER BY published_at DESC, post_id DESC
                    LIMIT :limit
                    """)
                    .param("ownerUserId", ownerUserId)
                    .param("limit", limit);
        }
        else {
            query = jdbcClient.sql("""
                    SELECT id, owner_user_id, post_id, author_id, published_at,
                           source_event_id, created_at
                    FROM timeline_entry
                    WHERE owner_user_id = :ownerUserId
                      AND (published_at < :cursorTime
                           OR (published_at = :cursorTime AND post_id < :cursorPostId))
                    ORDER BY published_at DESC, post_id DESC
                    LIMIT :limit
                    """)
                    .param("ownerUserId", ownerUserId)
                    .param("cursorTime", cursorTime.atOffset(ZoneOffset.UTC))
                    .param("cursorPostId", cursorPostId)
                    .param("limit", limit);
        }
        return query.query((resultSet, rowNumber) -> new TimelineEntry(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("owner_user_id", UUID.class),
                resultSet.getObject("post_id", UUID.class),
                resultSet.getObject("author_id", UUID.class),
                resultSet.getObject("published_at", java.time.OffsetDateTime.class).toInstant(),
                resultSet.getObject("source_event_id", UUID.class),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class).toInstant()))
                .list();
    }
}
