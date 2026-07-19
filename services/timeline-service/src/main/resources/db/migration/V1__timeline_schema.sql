CREATE TABLE timeline_entry (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    author_id UUID NOT NULL,
    published_at TIMESTAMPTZ(6) NOT NULL,
    source_event_id UUID NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT timeline_entry_owner_post_unique UNIQUE (owner_user_id, post_id)
);

CREATE INDEX timeline_entry_owner_keyset_idx
    ON timeline_entry (owner_user_id, published_at DESC, post_id DESC);

CREATE INDEX timeline_entry_owner_author_cleanup_idx
    ON timeline_entry (owner_user_id, author_id, published_at);

CREATE INDEX timeline_entry_post_cleanup_idx
    ON timeline_entry (post_id);
