CREATE TABLE attempt
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id          BIGINT NOT NULL,
    student_id       BIGINT NOT NULL,
    started_at       DATETIME,
    submitted_at     DATETIME,
    correct_count    INT,
    total_questions  INT,
    created_at       DATETIME,
    updated_at       DATETIME,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    deleted          BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE attempt_answer
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id  BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    choice_id   BIGINT,
    correct     BOOLEAN,

    CONSTRAINT fk_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempt (id),
    CONSTRAINT fk_attempt_answer_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT fk_attempt_answer_choice FOREIGN KEY (choice_id) REFERENCES choice (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
