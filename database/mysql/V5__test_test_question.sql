CREATE TABLE test
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NOT NULL,
    student_id BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_test_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_test_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE test_question
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id     BIGINT  NOT NULL,
    question_id BIGINT  NOT NULL,
    order_index INT     NOT NULL,

    CONSTRAINT fk_test_question_test FOREIGN KEY (test_id) REFERENCES test (id),
    CONSTRAINT fk_test_question_question FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
