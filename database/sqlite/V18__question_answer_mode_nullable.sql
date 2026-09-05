-- Bugfix (2026-09-05): answer_mode was added as NOT NULL DEFAULT 'AUDIO' in V11, but
-- QuestionService#normalizeAnswerMode deliberately returns NULL for MULTIPLE_CHOICE questions
-- (answer_mode is only meaningful for SPEAKING - see Question.java's javadoc). The application
-- always sends an explicit value for every column, so the column DEFAULT never kicks in for an
-- explicit NULL - every attempt to create a MULTIPLE_CHOICE question failed with a NOT NULL
-- constraint violation. SQLite has no ALTER COLUMN / DROP NOT NULL, so the standard rebuild
-- dance is required: recreate the table with the same columns (answer_mode now nullable), copy
-- the data across, drop the old table, rename the new one back. No indexes exist on this table
-- besides the implicit PK, so none need recreating.
PRAGMA foreign_keys=OFF;

CREATE TABLE question_new
(
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    lesson_id            INTEGER NOT NULL,
    content              TEXT    NOT NULL,
    knowledge_tag        TEXT,
    created_at           DATETIME,
    updated_at           DATETIME,
    created_by           TEXT,
    updated_by           TEXT,
    deleted              BOOLEAN NOT NULL DEFAULT 0,
    audio_path           TEXT,
    hide_content_in_test BOOLEAN NOT NULL DEFAULT 0,
    question_type        TEXT    NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    answer_mode          TEXT    DEFAULT 'AUDIO',
    reference_answer     TEXT,
    video_path           TEXT,

    CONSTRAINT fk_question_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
);

INSERT INTO question_new (id, lesson_id, content, knowledge_tag, created_at, updated_at, created_by,
                           updated_by, deleted, audio_path, hide_content_in_test, question_type,
                           answer_mode, reference_answer, video_path)
SELECT id, lesson_id, content, knowledge_tag, created_at, updated_at, created_by, updated_by,
       deleted, audio_path, hide_content_in_test, question_type, answer_mode, reference_answer,
       video_path
FROM question;

DROP TABLE question;
ALTER TABLE question_new RENAME TO question;

PRAGMA foreign_keys=ON;
