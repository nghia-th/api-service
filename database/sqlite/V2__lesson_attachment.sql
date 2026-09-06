-- See postgresql/V2__lesson_attachment.sql for the feature description.
CREATE TABLE lesson_attachment
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    lesson_id     INTEGER NOT NULL,
    original_name TEXT    NOT NULL,
    file_path     TEXT    NOT NULL,
    file_size     INTEGER NOT NULL,
    content_type  TEXT    NOT NULL,
    created_at    DATETIME,
    created_by    TEXT,

    CONSTRAINT fk_lesson_attachment_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
);
CREATE INDEX idx_lesson_attachment_lesson ON lesson_attachment (lesson_id);
