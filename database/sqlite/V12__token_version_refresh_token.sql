ALTER TABLE parent
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE student
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE refresh_token
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    role       TEXT    NOT NULL,
    token_hash TEXT    NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id, role);
