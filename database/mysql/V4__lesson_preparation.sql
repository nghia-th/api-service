-- "Prepared for tomorrow" checklist (2026-09-05, items 9/10 of the 11-item batch request). A row's
-- mere EXISTENCE means the student marked lesson_id as prepared for target_date - there is no
-- boolean flag column (unmarking is a plain DELETE, see LessonPreparationService.java). target_date
-- is a REAL calendar date, deliberately different from timetable_entry.day_of_week (a recurring
-- weekly template with no date) - "prepared for tomorrow" is about ONE specific upcoming date, so
-- this Monday's prep and next Monday's prep must never collide, and a prepared mark must never
-- retroactively change just because the weekly timetable template is edited afterward. The unique
-- constraint below is a DB-level backstop for the service-layer idempotency check in
-- LessonPreparationService#markPrepared (checks-then-inserts, but a unique index still protects
-- against a race between two concurrent requests). No classroom_id/subject_id columns - both
-- resolve indirectly via lesson_id, same "don't duplicate a derivable foreign key" reasoning as
-- timetable_entry.
CREATE TABLE lesson_preparation
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id   BIGINT  NOT NULL,
    target_date  DATE    NOT NULL,
    lesson_id    BIGINT  NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_lesson_preparation_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_lesson_preparation_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id),
    CONSTRAINT uq_lesson_preparation UNIQUE (student_id, target_date, lesson_id),
    INDEX idx_lesson_preparation_student_date (student_id, target_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
