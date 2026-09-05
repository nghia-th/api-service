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

CREATE TABLE subject
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    classroom_id INTEGER NOT NULL,
    name         TEXT    NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_subject_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
);

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
