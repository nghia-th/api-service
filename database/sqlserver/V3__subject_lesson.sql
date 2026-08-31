CREATE TABLE subject
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    parent_id  BIGINT        NOT NULL,
    name       NVARCHAR(255) NOT NULL,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT NOT NULL DEFAULT 0,

    CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);

CREATE TABLE lesson
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    subject_id BIGINT        NOT NULL,
    name       NVARCHAR(255) NOT NULL,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lesson_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);
