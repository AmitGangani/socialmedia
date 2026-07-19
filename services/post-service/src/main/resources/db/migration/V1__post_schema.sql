CREATE TABLE post (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL,
    text TEXT NOT NULL,
    published_at TIMESTAMPTZ(6) NOT NULL,
    parent_post_id UUID,
    parent_author_id UUID,
    deleted_at TIMESTAMPTZ(6),
    CONSTRAINT post_text_length CHECK (char_length(text) BETWEEN 1 AND 280),
    CONSTRAINT post_text_non_blank CHECK (btrim(text) <> ''),
    CONSTRAINT post_parent_pair CHECK (
        (parent_post_id IS NULL AND parent_author_id IS NULL)
        OR (parent_post_id IS NOT NULL AND parent_author_id IS NOT NULL)
    ),
    CONSTRAINT post_parent_fk FOREIGN KEY (parent_post_id) REFERENCES post (id) ON DELETE RESTRICT
);

CREATE INDEX post_author_keyset_idx
    ON post (author_id, published_at DESC, id DESC);

CREATE INDEX post_parent_idx
    ON post (parent_post_id)
    WHERE parent_post_id IS NOT NULL;

CREATE TABLE post_like (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT post_like_post_fk FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE RESTRICT,
    CONSTRAINT post_like_user_unique UNIQUE (post_id, user_id)
);

CREATE INDEX post_like_post_idx
    ON post_like (post_id);

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
    CONSTRAINT post_outbox_schema_version CHECK (schema_version = 1),
    CONSTRAINT post_outbox_topic CHECK (topic = 'post-events.v1'),
    CONSTRAINT post_outbox_event_type CHECK (event_type IN ('post.published.v1', 'post.deleted.v1')),
    CONSTRAINT post_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT post_outbox_attempt_count CHECK (attempt_count BETWEEN 0 AND 10),
    CONSTRAINT post_outbox_published_state CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR (status <> 'PUBLISHED' AND published_at IS NULL)
    )
);

CREATE INDEX post_outbox_due_idx
    ON outbox_event (status, next_attempt_at, created_at);

CREATE INDEX post_outbox_published_retention_idx
    ON outbox_event (published_at)
    WHERE status = 'PUBLISHED';

CREATE INDEX post_outbox_failed_retention_idx
    ON outbox_event (created_at)
    WHERE status = 'FAILED';
