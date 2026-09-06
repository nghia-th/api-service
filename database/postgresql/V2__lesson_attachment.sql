-- Lesson lecture-file attachments (2026-09-06, "phan bai cua mon hoc cho phep upload 1 hoac
-- nhieu file bai giang" feature) - a Lesson may have any number of PDF/PowerPoint files attached,
-- added/removed freely by the owning Parent. No BaseEntity audit suite (no edit/soft-delete
-- concept for a single attachment, same reasoning as subject_library_link) - just a lightweight
-- created_at/created_by pair.
CREATE TABLE lesson_attachment
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lesson_id     BIGINT       NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_path     VARCHAR(255) NOT NULL,
    file_size     BIGINT       NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP,
    created_by    VARCHAR(100),

    CONSTRAINT fk_lesson_attachment_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
);
CREATE INDEX idx_lesson_attachment_lesson ON lesson_attachment (lesson_id);
