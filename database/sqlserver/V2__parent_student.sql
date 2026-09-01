CREATE TABLE parent
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    full_name  NVARCHAR(255) NOT NULL,
    email      NVARCHAR(255) NOT NULL,
    password   NVARCHAR(255) NOT NULL,
    phone      NVARCHAR(20),
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT NOT NULL DEFAULT 0,

    CONSTRAINT uq_parent_email UNIQUE (email)
);

CREATE TABLE classroom
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    parent_id  BIGINT        NOT NULL,
    name       NVARCHAR(255) NOT NULL,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT NOT NULL DEFAULT 0,

    CONSTRAINT fk_classroom_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);

CREATE TABLE student
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    parent_id    BIGINT        NOT NULL,
    full_name    NVARCHAR(255) NOT NULL,
    classroom_id BIGINT        NOT NULL,
    username     NVARCHAR(100) NOT NULL,
    password     NVARCHAR(255) NOT NULL,
    created_at   DATETIME2,
    updated_at   DATETIME2,
    created_by   NVARCHAR(100),
    updated_by   NVARCHAR(100),
    deleted      BIT NOT NULL DEFAULT 0,

    CONSTRAINT uq_student_username UNIQUE (username),
    CONSTRAINT fk_student_parent FOREIGN KEY (parent_id) REFERENCES parent (id),
    CONSTRAINT fk_student_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
);
