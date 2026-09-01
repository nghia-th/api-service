CREATE TABLE attempt
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    test_id          BIGINT NOT NULL,
    student_id       BIGINT NOT NULL,
    started_at       TIMESTAMP,
    submitted_at     TIMESTAMP,
    correct_count    INTEGER,
    total_questions  INTEGER,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    deleted          BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student (id)
);

CREATE TABLE attempt_answer
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attempt_id  BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    choice_id   BIGINT,
    correct     BOOLEAN,

    CONSTRAINT fk_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempt (id),
    CONSTRAINT fk_attempt_answer_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT fk_attempt_answer_choice FOREIGN KEY (choice_id) REFERENCES choice (id)
);
