CREATE TABLE parent
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    phone      VARCHAR(20),
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_parent_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE classroom
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted    BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_classroom_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE student
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id    BIGINT       NOT NULL,
    full_name    VARCHAR(255) NOT NULL,
    classroom_id BIGINT       NOT NULL,
    username     VARCHAR(100) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_student_username UNIQUE (username),
    CONSTRAINT fk_student_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_student_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
