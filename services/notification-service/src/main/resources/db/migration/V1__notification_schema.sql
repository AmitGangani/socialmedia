CREATE TABLE notification (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    type VARCHAR(16) NOT NULL,
    subject_id UUID NOT NULL,
    parent_post_id UUID,
    event_time TIMESTAMPTZ(6) NOT NULL,
    source_event_id UUID NOT NULL,
    available_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT notification_type CHECK (type IN ('FOLLOW', 'REPLY')),
    CONSTRAINT notification_parent_shape CHECK (
        (type = 'FOLLOW' AND parent_post_id IS NULL)
        OR (type = 'REPLY' AND parent_post_id IS NOT NULL)
    ),
    CONSTRAINT notification_source_event_unique UNIQUE (source_event_id)
);

CREATE INDEX notification_recipient_keyset_idx
    ON notification (recipient_user_id, event_time DESC, id DESC);

CREATE TABLE processed_event (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT processed_event_pk PRIMARY KEY (consumer_name, event_id),
    CONSTRAINT processed_event_consumer CHECK (consumer_name = 'notification-service-v1')
);
