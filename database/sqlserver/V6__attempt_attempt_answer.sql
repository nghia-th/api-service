CREATE TABLE attempt
(
    id               BIGINT IDENTITY (1,1) PRIMARY KEY,
    test_id          BIGINT NOT NULL,
    student_id       BIGINT NOT NULL,
    started_at       DATETIME2,
    submitted_at     DATETIME2,
    correct_count    INT,
    total_questions  INT,
    created_at       DATETIME2,
    updated_at       DATETIME2,
    created_by       NVARCHAR(100),
    updated_by       NVARCHAR(100),
    deleted          BIT NOT NULL DEFAULT 0,

    CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student (id)
);

CREATE TABLE attempt_answer
(
    id          BIGINT IDENTITY (1,1) PRIMARY KEY,
    attempt_id  BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    choice_id   BIGINT,
    correct     BIT,

    CONSTRAINT fk_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempt (id),
    CONSTRAINT fk_attempt_answer_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT fk_attempt_answer_choice FOREIGN KEY (choice_id) REFERENCES choice (id)
);
