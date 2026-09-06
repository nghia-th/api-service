-- See postgresql/V2__lesson_attachment.sql for the feature description.
CREATE TABLE lesson_attachment
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id     BIGINT       NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_path     VARCHAR(255) NOT NULL,
    file_size     BIGINT       NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    created_at    DATETIME,
    created_by    VARCHAR(100),

    CONSTRAINT fk_lesson_attachment_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX idx_lesson_attachment_lesson ON lesson_attachment (lesson_id);
