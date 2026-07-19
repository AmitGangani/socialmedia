CREATE TABLE follow_relationship (
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL,
    followed_id UUID NOT NULL,
    followed_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT follow_relationship_distinct_users CHECK (follower_id <> followed_id),
    CONSTRAINT follow_relationship_pair_unique UNIQUE (follower_id, followed_id)
);

CREATE INDEX follow_relationship_followed_page_idx
    ON follow_relationship (followed_id, followed_at, follower_id);

CREATE INDEX follow_relationship_follower_count_idx
    ON follow_relationship (follower_id);

CREATE TABLE outbox_event (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    schema_version INTEGER NOT NULL,
    topic VARCHAR(128) NOT NULL,
    message_key VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ(6) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ(6) NOT NULL,
    published_at TIMESTAMPTZ(6),
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT follow_outbox_schema_version CHECK (schema_version = 1),
    CONSTRAINT follow_outbox_topic CHECK (topic = 'follow-events.v1'),
    CONSTRAINT follow_outbox_event_type CHECK (event_type IN ('follow.created.v1', 'follow.removed.v1')),
    CONSTRAINT follow_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT follow_outbox_attempt_count CHECK (attempt_count BETWEEN 0 AND 10),
    CONSTRAINT follow_outbox_published_state CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR (status <> 'PUBLISHED' AND published_at IS NULL)
    )
);

CREATE INDEX follow_outbox_due_idx
    ON outbox_event (status, next_attempt_at, created_at);

CREATE INDEX follow_outbox_published_retention_idx
    ON outbox_event (published_at)
    WHERE status = 'PUBLISHED';

CREATE INDEX follow_outbox_failed_retention_idx
    ON outbox_event (created_at)
    WHERE status = 'FAILED';
