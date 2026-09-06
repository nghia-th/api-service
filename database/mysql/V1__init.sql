-- Consolidated baseline (2026-09-06) - replaces the previous V1..V8 migration history.
-- quiz-service has not been deployed anywhere real yet (pre-production, per anh's confirmation),
-- so the 8 incremental migrations (curriculum, timetable, lesson_preparation, timetable_subject,
-- lesson_report, timetable_per_student, subject_shared_across_classrooms) are squashed into this
-- single file reflecting the CURRENT final schema, instead of replaying history that nobody but
-- this repo's own commits ever needs.
CREATE TABLE translate
(
    lang_key VARCHAR(255) NOT NULL,
    lang     VARCHAR(20)  NOT NULL,
    value    TEXT,

    PRIMARY KEY (lang_key, lang)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE parent
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    token_version INT          NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    username      VARCHAR(100),
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_parent_email UNIQUE (email),
    CONSTRAINT uq_parent_username UNIQUE (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE classroom
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_classroom_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE student
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id     BIGINT       NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    classroom_id  BIGINT       NOT NULL,
    username      VARCHAR(100) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    token_version INT          NOT NULL DEFAULT 0,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_student_username UNIQUE (username),
    CONSTRAINT fk_student_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_student_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Subject: classroom_id is NULLABLE - NULL means "shared across every Classroom of this Parent"
-- (subject_shared_across_classrooms, 2026-09-06). parent_id is the real ownership FK.
CREATE TABLE subject
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id BIGINT,
    parent_id    BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_subject_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id),
    CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    INDEX idx_subject_parent (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE lesson
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id    BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    summary       TEXT,
    content       TEXT,
    textbook_page INT,
    image_path    VARCHAR(255),
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_lesson_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE question
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id            BIGINT      NOT NULL,
    content              TEXT        NOT NULL,
    knowledge_tag        VARCHAR(255),
    audio_path           VARCHAR(255),
    hide_content_in_test BOOLEAN     NOT NULL DEFAULT FALSE,
    question_type        VARCHAR(20) NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    answer_mode          VARCHAR(20) DEFAULT 'AUDIO',
    reference_answer     TEXT,
    video_path           VARCHAR(255),
    created_at           DATETIME,
    updated_at           DATETIME,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    deleted              BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_question_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE choice
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT  NOT NULL,
    content     TEXT    NOT NULL,
    correct     BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_choice_question FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE test
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NOT NULL,
    student_id BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    test_type  VARCHAR(20)  NOT NULL DEFAULT 'REGULAR',
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_test_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_test_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE test_question
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id     BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    order_index INT    NOT NULL,

    CONSTRAINT fk_test_question_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_test_question_question FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE attempt
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id         BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    started_at      DATETIME,
    submitted_at    DATETIME,
    correct_count   INT,
    total_questions INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_attempt_test_id UNIQUE (test_id),
    CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE attempt_answer
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id            BIGINT NOT NULL,
    question_id           BIGINT NOT NULL,
    choice_id             BIGINT,
    correct               BOOLEAN,
    answer_audio_path     VARCHAR(255),
    parent_marked_correct BOOLEAN,
    answer_text           TEXT,

    CONSTRAINT fk_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempt (id),
    CONSTRAINT fk_attempt_answer_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT fk_attempt_answer_choice FOREIGN KEY (choice_id) REFERENCES choice (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE refresh_token
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at DATETIME     NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id, role);

CREATE TABLE admin
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    token_version INT          NOT NULL DEFAULT 0,
    root          BOOLEAN      NOT NULL DEFAULT FALSE,
    username      VARCHAR(100),
    phone         VARCHAR(20),
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_admin_email UNIQUE (email),
    CONSTRAINT uq_admin_username UNIQUE (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE library_document
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    grade        INT          NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    curriculum   VARCHAR(100) NOT NULL,
    volume       VARCHAR(100),
    title        VARCHAR(255) NOT NULL,
    file_path    VARCHAR(255) NOT NULL,
    file_size    BIGINT       NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE subject_library_link
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id          BIGINT NOT NULL,
    library_document_id BIGINT NOT NULL,
    linked_at           DATETIME,
    linked_by           VARCHAR(100),

    CONSTRAINT uq_subject_library_link UNIQUE (subject_id, library_document_id),
    CONSTRAINT fk_subject_library_link_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_subject_library_link_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Admin-managed "bo sach" (curriculum) lookup list - LibraryDocument.curriculum keeps storing the
-- plain name (not a foreign key - see Curriculum.java's javadoc).
CREATE TABLE curriculum
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_curriculum_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Weekly timetable ("thoi khoa bieu") per Student (not per Classroom - 2 siblings in the same
-- Classroom can have different schedules). Single persistent template (no per-week snapshot) -
-- day_of_week is 1-7 Monday-Sunday (java.time.DayOfWeek#getValue(), ISO-8601). Pins a Subject
-- (not an exact Lesson) - order_index alone controls display order within a day.
CREATE TABLE timetable_entry
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT  NOT NULL,
    day_of_week INT     NOT NULL,
    subject_id  BIGINT  NOT NULL,
    order_index INT     NOT NULL,
    created_at  DATETIME,
    updated_at  DATETIME,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_timetable_entry_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_timetable_entry_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    INDEX idx_timetable_entry_student_day (student_id, day_of_week)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- "Prepared for tomorrow" checklist - a row's mere EXISTENCE means the Student marked subject_id
-- as prepared for target_date (unmarking is a plain DELETE, no boolean flag column).
CREATE TABLE lesson_preparation
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT  NOT NULL,
    target_date DATE    NOT NULL,
    subject_id  BIGINT  NOT NULL,
    created_at  DATETIME,
    updated_at  DATETIME,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_lesson_preparation_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_lesson_preparation_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT uq_lesson_preparation UNIQUE (student_id, target_date, subject_id),
    INDEX idx_lesson_preparation_student_date (student_id, target_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- "Bao bai" - a real log of a Student confirming they finished a specific Lesson on a specific
-- report_date, kept forever as history for the Parent to review. UNIQUE(student_id, lesson_id) -
-- a Lesson once reported (any date) is hidden from that Student's pick list forever after.
CREATE TABLE lesson_report
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT  NOT NULL,
    lesson_id   BIGINT  NOT NULL,
    report_date DATE    NOT NULL,
    created_at  DATETIME,
    updated_at  DATETIME,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_lesson_report_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_lesson_report_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id),
    CONSTRAINT uq_lesson_report_student_lesson UNIQUE (student_id, lesson_id),
    INDEX idx_lesson_report_student_date (student_id, report_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
