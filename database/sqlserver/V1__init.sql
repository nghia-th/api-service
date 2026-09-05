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

CREATE TABLE subject
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    classroom_id BIGINT        NOT NULL,
    name         NVARCHAR(255) NOT NULL,
    created_at   DATETIME2,
    updated_at   DATETIME2,
    created_by   NVARCHAR(100),
    updated_by   NVARCHAR(100),
    deleted      BIT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_subject_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
);

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
