ALTER TABLE parent
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE admin
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    token_version INT          NOT NULL DEFAULT 0,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_admin_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
