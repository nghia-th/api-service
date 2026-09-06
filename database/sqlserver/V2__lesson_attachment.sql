-- See postgresql/V2__lesson_attachment.sql for the feature description.
CREATE TABLE lesson_attachment
(
    id            BIGINT IDENTITY (1,1) PRIMARY KEY,
    lesson_id     BIGINT        NOT NULL,
    original_name NVARCHAR(255) NOT NULL,
    file_path     NVARCHAR(255) NOT NULL,
    file_size     BIGINT        NOT NULL,
    content_type  NVARCHAR(100) NOT NULL,
    created_at    DATETIME2,
    created_by    NVARCHAR(100),

    CONSTRAINT fk_lesson_attachment_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
);
CREATE INDEX idx_lesson_attachment_lesson ON lesson_attachment (lesson_id);
