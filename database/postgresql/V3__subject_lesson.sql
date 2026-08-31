CREATE TABLE subject
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id  BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);

CREATE TABLE lesson
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_id BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_lesson_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);
