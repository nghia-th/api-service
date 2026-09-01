CREATE TABLE subject
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_subject_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE lesson
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_lesson_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
