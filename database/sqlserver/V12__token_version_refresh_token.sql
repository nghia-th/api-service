ALTER TABLE parent
    ADD token_version INT NOT NULL DEFAULT 0;

ALTER TABLE student
    ADD token_version INT NOT NULL DEFAULT 0;

CREATE TABLE refresh_token
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    user_id    BIGINT         NOT NULL,
    role       NVARCHAR(20)   NOT NULL,
    token_hash NVARCHAR(128)  NOT NULL,
    expires_at DATETIME2      NOT NULL,
    revoked    BIT            NOT NULL DEFAULT 0,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT            NOT NULL DEFAULT 0,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id, role);
