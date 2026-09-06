-- Consolidated baseline (2026-09-06) - replaces the previous V1..V8 migration history.
-- quiz-service has not been deployed anywhere real yet (pre-production, per anh's confirmation),
-- so the 8 incremental migrations (curriculum, timetable, lesson_preparation, timetable_subject,
-- lesson_report, timetable_per_student, subject_shared_across_classrooms) are squashed into this
-- single file reflecting the CURRENT final schema, instead of replaying history that nobody but
-- this repo's own commits ever needs.
CREATE TABLE translate
(
    lang_key NVARCHAR(255) NOT NULL,
    lang     NVARCHAR(20)  NOT NULL,
    value    NVARCHAR(MAX),

    CONSTRAINT PK_translate PRIMARY KEY (lang_key, lang)
);

CREATE TABLE parent
(
    id            BIGINT IDENTITY (1,1) PRIMARY KEY,
    full_name     NVARCHAR(255) NOT NULL,
    email         NVARCHAR(255) NOT NULL,
    password      NVARCHAR(255) NOT NULL,
    phone         NVARCHAR(20),
    token_version INT           NOT NULL DEFAULT 0,
    active        BIT           NOT NULL DEFAULT 1,
    username      NVARCHAR(100),
    created_at    DATETIME2,
    updated_at    DATETIME2,
    created_by    NVARCHAR(100),
    updated_by    NVARCHAR(100),
    deleted       BIT           NOT NULL DEFAULT 0,

    CONSTRAINT uq_parent_email UNIQUE (email)
);

-- SQL Server's plain UNIQUE constraint allows only a single NULL, which would break as soon as a
-- 2nd parent had no username yet - a filtered unique index (WHERE username IS NOT NULL) is the
-- standard workaround, enforcing uniqueness only among rows that actually have a username set.
CREATE UNIQUE INDEX uq_parent_username ON parent (username) WHERE username IS NOT NULL;

CREATE TABLE classroom
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    parent_id  BIGINT        NOT NULL,
    name       NVARCHAR(255) NOT NULL,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_classroom_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);

CREATE TABLE student
(
    id            BIGINT IDENTITY (1,1) PRIMARY KEY,
    parent_id     BIGINT        NOT NULL,
    full_name     NVARCHAR(255) NOT NULL,
    classroom_id  BIGINT        NOT NULL,
    username      NVARCHAR(100) NOT NULL,
    password      NVARCHAR(255) NOT NULL,
    token_version INT           NOT NULL DEFAULT 0,
    created_at    DATETIME2,
    updated_at    DATETIME2,
    created_by    NVARCHAR(100),
    updated_by    NVARCHAR(100),
    deleted       BIT           NOT NULL DEFAULT 0,

    CONSTRAINT uq_student_username UNIQUE (username),
    CONSTRAINT fk_student_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_student_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
);

-- Subject: classroom_id is NULLABLE - NULL means "shared across every Classroom of this Parent"
-- (subject_shared_across_classrooms, 2026-09-06). parent_id is the real ownership FK.
CREATE TABLE subject
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    classroom_id BIGINT,
    parent_id    BIGINT        NOT NULL,
    name         NVARCHAR(255) NOT NULL,
    created_at   DATETIME2,
    updated_at   DATETIME2,
    created_by   NVARCHAR(100),
    updated_by   NVARCHAR(100),
    deleted      BIT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_subject_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id),
    CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);
CREATE INDEX idx_subject_parent ON subject (parent_id);

CREATE TABLE lesson
(
    id            BIGINT IDENTITY (1,1) PRIMARY KEY,
    subject_id    BIGINT        NOT NULL,
    name          NVARCHAR(255) NOT NULL,
    summary       NVARCHAR(MAX),
    content       NVARCHAR(MAX),
    textbook_page INT,
    image_path    NVARCHAR(255),
    created_at    DATETIME2,
    updated_at    DATETIME2,
    created_by    NVARCHAR(100),
    updated_by    NVARCHAR(100),
    deleted       BIT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_lesson_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);

CREATE TABLE question
(
    id                   BIGINT IDENTITY (1,1) PRIMARY KEY,
    lesson_id            BIGINT        NOT NULL,
    content              NVARCHAR(MAX) NOT NULL,
    knowledge_tag        NVARCHAR(255),
    audio_path           NVARCHAR(255),
    hide_content_in_test BIT           NOT NULL DEFAULT 0,
    question_type        NVARCHAR(20)  NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    answer_mode          NVARCHAR(20)  DEFAULT 'AUDIO' NULL,
    reference_answer     NVARCHAR(MAX),
    video_path           NVARCHAR(255),
    created_at           DATETIME2,
    updated_at           DATETIME2,
    created_by           NVARCHAR(100),
    updated_by           NVARCHAR(100),
    deleted              BIT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_question_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
);

CREATE TABLE choice
(
    id          BIGINT IDENTITY (1,1) PRIMARY KEY,
    question_id BIGINT        NOT NULL,
    content     NVARCHAR(MAX) NOT NULL,
    correct     BIT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_choice_question FOREIGN KEY (question_id) REFERENCES question (id)
);

CREATE TABLE test
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    parent_id  BIGINT        NOT NULL,
    student_id BIGINT        NOT NULL,
    name       NVARCHAR(255) NOT NULL,
    status     NVARCHAR(20)  NOT NULL,
    test_type  NVARCHAR(20)  NOT NULL DEFAULT 'REGULAR',
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_test_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_test_student FOREIGN KEY (student_id) REFERENCES student (id)
);

CREATE TABLE test_question
(
    id          BIGINT IDENTITY (1,1) PRIMARY KEY,
    test_id     BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    order_index INT    NOT NULL,

    CONSTRAINT fk_test_question_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_test_question_question FOREIGN KEY (question_id) REFERENCES question (id)
);

CREATE TABLE attempt
(
    id              BIGINT IDENTITY (1,1) PRIMARY KEY,
    test_id         BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    started_at      DATETIME2,
    submitted_at    DATETIME2,
    correct_count   INT,
    total_questions INT,
    created_at      DATETIME2,
    updated_at      DATETIME2,
    created_by      NVARCHAR(100),
    updated_by      NVARCHAR(100),
    deleted         BIT NOT NULL DEFAULT 0,

    CONSTRAINT uq_attempt_test_id UNIQUE (test_id),
    CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student (id)
);

CREATE TABLE attempt_answer
(
    id                    BIGINT IDENTITY (1,1) PRIMARY KEY,
    attempt_id            BIGINT NOT NULL,
    question_id           BIGINT NOT NULL,
    choice_id             BIGINT,
    correct               BIT,
    answer_audio_path     NVARCHAR(255),
    parent_marked_correct BIT,
    answer_text           NVARCHAR(MAX),

    CONSTRAINT fk_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempt (id),
    CONSTRAINT fk_attempt_answer_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT fk_attempt_answer_choice FOREIGN KEY (choice_id) REFERENCES choice (id)
);

CREATE TABLE refresh_token
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    role       NVARCHAR(20)  NOT NULL,
    token_hash NVARCHAR(128) NOT NULL,
    expires_at DATETIME2     NOT NULL,
    revoked    BIT           NOT NULL DEFAULT 0,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT           NOT NULL DEFAULT 0,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id, role);

CREATE TABLE admin
(
    id            BIGINT IDENTITY (1,1) PRIMARY KEY,
    email         NVARCHAR(255) NOT NULL,
    password      NVARCHAR(255) NOT NULL,
    full_name     NVARCHAR(255) NOT NULL,
    token_version INT           NOT NULL DEFAULT 0,
    root          BIT           NOT NULL DEFAULT 0,
    username      NVARCHAR(100),
    phone         NVARCHAR(20),
    created_at    DATETIME2,
    updated_at    DATETIME2,
    created_by    NVARCHAR(100),
    updated_by    NVARCHAR(100),
    deleted       BIT           NOT NULL DEFAULT 0,

    CONSTRAINT uq_admin_email UNIQUE (email)
);

CREATE UNIQUE INDEX uq_admin_username ON admin (username) WHERE username IS NOT NULL;

CREATE TABLE library_document
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    grade        INT           NOT NULL,
    subject_name NVARCHAR(255) NOT NULL,
    curriculum   NVARCHAR(100) NOT NULL,
    volume       NVARCHAR(100),
    title        NVARCHAR(255) NOT NULL,
    file_path    NVARCHAR(255) NOT NULL,
    file_size    BIGINT        NOT NULL,
    created_at   DATETIME2,
    updated_at   DATETIME2,
    created_by   NVARCHAR(100),
    updated_by   NVARCHAR(100),
    deleted      BIT           NOT NULL DEFAULT 0
);

CREATE TABLE subject_library_link
(
    id                  BIGINT IDENTITY (1,1) PRIMARY KEY,
    subject_id          BIGINT NOT NULL,
    library_document_id BIGINT NOT NULL,
    linked_at           DATETIME2,
    linked_by           NVARCHAR(100),

    CONSTRAINT uq_subject_library_link UNIQUE (subject_id, library_document_id),
    CONSTRAINT fk_subject_library_link_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_subject_library_link_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
);

-- Admin-managed "bo sach" (curriculum) lookup list - LibraryDocument.curriculum keeps storing the
-- plain name (not a foreign key - see Curriculum.java's javadoc).
CREATE TABLE curriculum
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    name       NVARCHAR(255) NOT NULL,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT           NOT NULL DEFAULT 0,

    CONSTRAINT uq_curriculum_name UNIQUE (name)
);

-- Weekly timetable ("thoi khoa bieu") per Student (not per Classroom - 2 siblings in the same
-- Classroom can have different schedules). Single persistent template (no per-week snapshot) -
-- day_of_week is 1-7 Monday-Sunday (java.time.DayOfWeek#getValue(), ISO-8601). Pins a Subject
-- (not an exact Lesson) - order_index alone controls display order within a day.
CREATE TABLE timetable_entry
(
    id          BIGINT IDENTITY (1,1) PRIMARY KEY,
    student_id  BIGINT NOT NULL,
    day_of_week INT    NOT NULL,
    subject_id  BIGINT NOT NULL,
    order_index INT    NOT NULL,
    created_at  DATETIME2,
    updated_at  DATETIME2,
    created_by  NVARCHAR(100),
    updated_by  NVARCHAR(100),
    deleted     BIT    NOT NULL DEFAULT 0,

    CONSTRAINT fk_timetable_entry_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_timetable_entry_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);
CREATE INDEX idx_timetable_entry_student_day ON timetable_entry (student_id, day_of_week);

-- "Prepared for tomorrow" checklist - a row's mere EXISTENCE means the Student marked subject_id
-- as prepared for target_date (unmarking is a plain DELETE, no boolean flag column).
CREATE TABLE lesson_preparation
(
    id          BIGINT IDENTITY (1,1) PRIMARY KEY,
    student_id  BIGINT NOT NULL,
    target_date DATE   NOT NULL,
    subject_id  BIGINT NOT NULL,
    created_at  DATETIME2,
    updated_at  DATETIME2,
    created_by  NVARCHAR(100),
    updated_by  NVARCHAR(100),
    deleted     BIT    NOT NULL DEFAULT 0,

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
    id          BIGINT IDENTITY (1,1) PRIMARY KEY,
    student_id  BIGINT NOT NULL,
    lesson_id   BIGINT NOT NULL,
    report_date DATE   NOT NULL,
    created_at  DATETIME2,
    updated_at  DATETIME2,
    created_by  NVARCHAR(100),
    updated_by  NVARCHAR(100),
    deleted     BIT    NOT NULL DEFAULT 0,

    CONSTRAINT fk_lesson_report_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_lesson_report_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id),
    CONSTRAINT uq_lesson_report_student_lesson UNIQUE (student_id, lesson_id)
);
CREATE INDEX idx_lesson_report_student_date ON lesson_report (student_id, report_date);
