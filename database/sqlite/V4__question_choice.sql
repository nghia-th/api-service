CREATE TABLE question
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    lesson_id     INTEGER NOT NULL,
    content       TEXT    NOT NULL,
    knowledge_tag TEXT,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    TEXT,
    updated_by    TEXT,
    deleted       BOOLEAN NOT NULL DEFAULT 0,

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
