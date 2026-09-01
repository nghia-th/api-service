CREATE TABLE question
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lesson_id     BIGINT NOT NULL,
    content       TEXT   NOT NULL,
    knowledge_tag VARCHAR(255),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_question_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
);

CREATE TABLE choice
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id BIGINT  NOT NULL,
    content     TEXT    NOT NULL,
    correct     BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_choice_question FOREIGN KEY (question_id) REFERENCES question (id)
);
