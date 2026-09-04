ALTER TABLE parent
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE student
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE refresh_token
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id, role);
