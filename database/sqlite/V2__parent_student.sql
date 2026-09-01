CREATE TABLE parent
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name  TEXT    NOT NULL,
    email      TEXT    NOT NULL,
    password   TEXT    NOT NULL,
    phone      TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_parent_email UNIQUE (email)
);

CREATE TABLE classroom
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id  INTEGER NOT NULL,
    name       TEXT    NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by TEXT,
    updated_by TEXT,
    deleted    BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_classroom_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);

CREATE TABLE student
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id    INTEGER NOT NULL,
    full_name    TEXT    NOT NULL,
    classroom_id INTEGER NOT NULL,
    username     TEXT    NOT NULL,
    password     TEXT    NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT uq_student_username UNIQUE (username),
    CONSTRAINT fk_student_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_student_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
);
