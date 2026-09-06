-- Consolidated baseline (2026-09-06) - replaces the previous V1..V8 migration history.
-- quiz-service has not been deployed anywhere real yet (pre-production, per anh's confirmation),
-- so the 8 incremental migrations (curriculum, timetable, lesson_preparation, timetable_subject,
-- lesson_report, timetable_per_student, subject_shared_across_classrooms) are squashed into this
-- single file reflecting the CURRENT final schema, instead of replaying history that nobody but
-- this repo's own commits ever needs. Local dev SQLite data was wiped (data/app.db*) as part of
-- this change, per anh's explicit confirmation, since Flyway's checksum for a from-scratch V1
-- would otherwise mismatch what was already recorded for the old V1..V8 files.
CREATE TABLE translate
(
    lang_key TEXT NOT NULL,
    lang     TEXT NOT NULL,
    value    TEXT,

    PRIMARY KEY (lang_key, lang)
);

CREATE TABLE parent
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name     TEXT    NOT NULL,
    email         TEXT    NOT NULL,
    password      TEXT    NOT NULL,
    phone         TEXT,
    token_version INTEGER NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT 1,
    username      TEXT,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    TEXT,
    updated_by    TEXT,
    deleted       BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_parent_email UNIQUE (email),
    CONSTRAINT uq_parent_username UNIQUE (username)
);

CREATE TABLE classroom
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id  INTEGER NOT NULL,
    name       TEXT    NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_classroom_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);

CREATE TABLE student
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id     INTEGER NOT NULL,
    full_name     TEXT    NOT NULL,
    classroom_id  INTEGER NOT NULL,
    username      TEXT    NOT NULL,
    password      TEXT    NOT NULL,
    token_version INTEGER NOT NULL DEFAULT 0,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    TEXT,
    updated_by    TEXT,
    deleted       BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_student_username UNIQUE (username),
    CONSTRAINT fk_student_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_student_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
);

-- Subject: classroom_id is NULLABLE - NULL means "shared across every Classroom of this Parent"
-- (subject_shared_across_classrooms, 2026-09-06). parent_id is the real ownership FK (added
-- alongside classroom_id becoming nullable, since ownership can no longer always be derived via
-- classroom_id -> classroom.parent_id).
CREATE TABLE subject
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    classroom_id INTEGER,
    parent_id    INTEGER NOT NULL,
    name         TEXT    NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_subject_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id),
    CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);
CREATE INDEX idx_subject_parent ON subject (parent_id);

CREATE TABLE lesson
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id    INTEGER NOT NULL,
    name          TEXT    NOT NULL,
    summary       TEXT,
    content       TEXT,
    textbook_page INTEGER,
    image_path    TEXT,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    TEXT,
    updated_by    TEXT,
    deleted       BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_lesson_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);

CREATE TABLE question
(
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    lesson_id            INTEGER NOT NULL,
    content              TEXT    NOT NULL,
    knowledge_tag        TEXT,
    audio_path           TEXT,
    hide_content_in_test BOOLEAN NOT NULL DEFAULT 0,
    question_type        TEXT    NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    answer_mode          TEXT    DEFAULT 'AUDIO',
    reference_answer     TEXT,
    video_path           TEXT,
    created_at           DATETIME,
    updated_at           DATETIME,
    created_by           TEXT,
    updated_by            TEXT,
    deleted              BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_question_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
);

CREATE TABLE choice
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    question_id INTEGER NOT NULL,
    content     TEXT    NOT NULL,
    correct     BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_choice_question FOREIGN KEY (question_id) REFERENCES question (id)
);

CREATE TABLE test
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id  INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    name       TEXT    NOT NULL,
    status     TEXT    NOT NULL,
    test_type  TEXT    NOT NULL DEFAULT 'REGULAR',
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_test_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_test_student FOREIGN KEY (student_id) REFERENCES student (id)
);

CREATE TABLE test_question
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    test_id     INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    order_index INTEGER NOT NULL,

    CONSTRAINT fk_test_question_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_test_question_question FOREIGN KEY (question_id) REFERENCES question (id)
);

CREATE TABLE attempt
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    test_id         INTEGER NOT NULL,
    student_id      INTEGER NOT NULL,
    started_at      DATETIME,
    submitted_at    DATETIME,
    correct_count   INTEGER,
    total_questions INTEGER,
    created_at      DATETIME,
    updated_at      DATETIME,
    created_by      TEXT,
    updated_by      TEXT,
    deleted         BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_attempt_test_id UNIQUE (test_id),
    CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student (id)
);

CREATE TABLE attempt_answer
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    attempt_id            INTEGER NOT NULL,
    question_id           INTEGER NOT NULL,
    choice_id             INTEGER,
    correct               BOOLEAN,
    answer_audio_path     TEXT,
    parent_marked_correct BOOLEAN,
    answer_text           TEXT,

    CONSTRAINT fk_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempt (id),
    CONSTRAINT fk_attempt_answer_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT fk_attempt_answer_choice FOREIGN KEY (choice_id) REFERENCES choice (id)
);

CREATE TABLE refresh_token
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    role       TEXT    NOT NULL,
    token_hash TEXT    NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id, role);

CREATE TABLE admin
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    email         TEXT    NOT NULL,
    password      TEXT    NOT NULL,
    full_name     TEXT    NOT NULL,
    token_version INTEGER NOT NULL DEFAULT 0,
    root          BOOLEAN NOT NULL DEFAULT 0,
    username      TEXT,
    phone         TEXT,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    TEXT,
    updated_by    TEXT,
    deleted       BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_admin_email UNIQUE (email),
    CONSTRAINT uq_admin_username UNIQUE (username)
);

CREATE TABLE library_document
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    grade        INTEGER NOT NULL,
    subject_name TEXT    NOT NULL,
    curriculum   TEXT    NOT NULL,
    volume       TEXT,
    title        TEXT    NOT NULL,
    file_path    TEXT    NOT NULL,
    file_size    INTEGER NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE subject_library_link
(
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id          INTEGER NOT NULL,
    library_document_id INTEGER NOT NULL,
    linked_at           DATETIME,
    linked_by           TEXT,

    CONSTRAINT uq_subject_library_link UNIQUE (subject_id, library_document_id),
    CONSTRAINT fk_subject_library_link_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_subject_library_link_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
);

-- Admin-managed "bo sach" (curriculum) lookup list - LibraryDocument.curriculum keeps storing the
-- plain name (not a foreign key - see Curriculum.java's javadoc).
CREATE TABLE curriculum
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_curriculum_name UNIQUE (name)
);

-- Weekly timetable ("thoi khoa bieu") per Student (not per Classroom - 2 siblings in the same
-- Classroom can have different schedules). Single persistent template (no per-week snapshot) -
-- day_of_week is 1-7 Monday-Sunday (java.time.DayOfWeek#getValue(), ISO-8601). Pins a Subject
-- (not an exact Lesson) - order_index alone controls display order within a day.
CREATE TABLE timetable_entry
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id  INTEGER NOT NULL,
    day_of_week INTEGER NOT NULL,
    subject_id  INTEGER NOT NULL,
    order_index INTEGER NOT NULL,
    created_at  DATETIME,
    updated_at  DATETIME,
    created_by  TEXT,
    updated_by  TEXT,
    deleted     BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_timetable_entry_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_timetable_entry_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);
CREATE INDEX idx_timetable_entry_student_day ON timetable_entry (student_id, day_of_week);

-- "Prepared for tomorrow" checklist - a row's mere EXISTENCE means the Student marked subject_id
-- as prepared for target_date (unmarking is a plain DELETE, no boolean flag column).
CREATE TABLE lesson_preparation
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id  INTEGER NOT NULL,
    target_date DATE    NOT NULL,
    subject_id  INTEGER NOT NULL,
    created_at  DATETIME,
    updated_at  DATETIME,
    created_by  TEXT,
    updated_by  TEXT,
    deleted     BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_lesson_preparation_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_lesson_preparation_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT uq_lesson_preparation UNIQUE (student_id, target_date, subject_id)
);
CREATE INDEX idx_lesson_preparation_student_date ON lesson_preparation (student_id, target_date);

-- "Bao bai" - a real log of a Student confirming they finished a specific Lesson on a specific
-- report_date, kept forever as history for the Parent to review. UNIQUE(student_id, lesson_id) -
-- a Lesson once reported (any date) is hidden from that Student's pick list forever after.
CREATE TABLE lesson_report
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id  INTEGER NOT NULL,
    lesson_id   INTEGER NOT NULL,
    report_date DATE    NOT NULL,
    created_at  DATETIME,
    updated_at  DATETIME,
    created_by  TEXT,
    updated_by  TEXT,
    deleted     BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_lesson_report_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_lesson_report_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id),
    CONSTRAINT uq_lesson_report_student_lesson UNIQUE (student_id, lesson_id)
);
CREATE INDEX idx_lesson_report_student_date ON lesson_report (student_id, report_date);
