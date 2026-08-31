CREATE TABLE subject
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id  INTEGER NOT NULL,
    name       TEXT    NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);

CREATE TABLE lesson
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id INTEGER NOT NULL,
    name       TEXT    NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_lesson_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);
