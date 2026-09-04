ALTER TABLE parent
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT 1;

CREATE TABLE admin
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    email         TEXT    NOT NULL,
    password      TEXT    NOT NULL,
    full_name     TEXT    NOT NULL,
    token_version INTEGER NOT NULL DEFAULT 0,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    TEXT,
    updated_by    TEXT,
    deleted       BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_admin_email UNIQUE (email)
);
