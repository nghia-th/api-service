ALTER TABLE parent
    ADD active BIT NOT NULL DEFAULT 1;

CREATE TABLE admin
(
    id            BIGINT IDENTITY (1,1) PRIMARY KEY,
    email         NVARCHAR(255) NOT NULL,
    password      NVARCHAR(255) NOT NULL,
    full_name     NVARCHAR(255) NOT NULL,
    token_version INT           NOT NULL DEFAULT 0,
    created_at    DATETIME2,
    updated_at    DATETIME2,
    created_by    NVARCHAR(100),
    updated_by    NVARCHAR(100),
    deleted       BIT           NOT NULL DEFAULT 0,

    CONSTRAINT uq_admin_email UNIQUE (email)
);
