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

CREATE TABLE subject
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_subject_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
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
