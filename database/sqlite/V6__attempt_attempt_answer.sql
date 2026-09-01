CREATE TABLE attempt
(
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    test_id          INTEGER NOT NULL,
    student_id       INTEGER NOT NULL,
    started_at       DATETIME,
    submitted_at     DATETIME,
    correct_count    INTEGER,
    total_questions  INTEGER,
    created_at       DATETIME,
    updated_at       DATETIME,
    created_by       TEXT,
    updated_by       TEXT,
    deleted          BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student (id)
);

CREATE TABLE attempt_answer
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    attempt_id  INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    choice_id   INTEGER,
    correct     BOOLEAN,

    CONSTRAINT fk_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempt (id),
    CONSTRAINT fk_attempt_answer_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT fk_attempt_answer_choice FOREIGN KEY (choice_id) REFERENCES choice (id)
);
