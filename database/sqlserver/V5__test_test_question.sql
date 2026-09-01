CREATE TABLE test
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    parent_id  BIGINT        NOT NULL,
    student_id BIGINT        NOT NULL,
    name       NVARCHAR(255) NOT NULL,
    status     NVARCHAR(20)  NOT NULL,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT NOT NULL DEFAULT 0,

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
