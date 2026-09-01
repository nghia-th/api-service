CREATE TABLE question
(
    id            BIGINT IDENTITY (1,1) PRIMARY KEY,
    lesson_id     BIGINT        NOT NULL,
    content       NVARCHAR(MAX) NOT NULL,
    knowledge_tag NVARCHAR(255),
    created_at    DATETIME2,
    updated_at    DATETIME2,
    created_by    NVARCHAR(100),
    updated_by    NVARCHAR(100),
    deleted       BIT NOT NULL DEFAULT 0,

    CONSTRAINT fk_question_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
);

CREATE TABLE choice
(
    id          BIGINT IDENTITY (1,1) PRIMARY KEY,
    question_id BIGINT        NOT NULL,
    content     NVARCHAR(MAX) NOT NULL,
    correct     BIT NOT NULL DEFAULT 0,

    CONSTRAINT fk_choice_question FOREIGN KEY (question_id) REFERENCES question (id)
);
