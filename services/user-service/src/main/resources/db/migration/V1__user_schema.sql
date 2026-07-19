CREATE TABLE account (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    normalized_email VARCHAR(254) NOT NULL,
    username VARCHAR(30) NOT NULL,
    normalized_username VARCHAR(30) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    bio VARCHAR(160) NOT NULL DEFAULT '',
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT account_email_length CHECK (char_length(email) BETWEEN 3 AND 254),
    CONSTRAINT account_username_format CHECK (username ~ '^[A-Za-z0-9_]{3,30}$'),
    CONSTRAINT account_normalized_username_format CHECK (normalized_username ~ '^[a-z0-9_]{3,30}$'),
    CONSTRAINT account_display_name_length CHECK (char_length(display_name) BETWEEN 1 AND 80),
    CONSTRAINT account_bio_length CHECK (char_length(bio) <= 160)
);

CREATE UNIQUE INDEX account_normalized_email_unique
    ON account (normalized_email);

CREATE UNIQUE INDEX account_normalized_username_unique
    ON account (normalized_username);
