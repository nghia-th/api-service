CREATE TABLE question
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id     BIGINT NOT NULL,
    content       TEXT   NOT NULL,
    knowledge_tag VARCHAR(255),
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,

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
