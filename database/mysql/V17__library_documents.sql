-- Textbook library feature (2026-09-05): Admin uploads PDF textbooks organized by
-- Grade -> Subject name -> Curriculum series -> Volume (e.g. "Lop 4 -> Toan tap 1 -> Ket noi tri
-- thuc"). A Parent's own Subject rows can each link to MULTIPLE library documents
-- (subject_library_link is a many-to-many join, see LibraryService's javadoc) - both Parent and
-- Student can then view/download a linked document.
CREATE TABLE library_document
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    grade        INT          NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    curriculum   VARCHAR(100) NOT NULL,
    volume       VARCHAR(100),
    title        VARCHAR(255) NOT NULL,
    file_path    VARCHAR(255) NOT NULL,
    file_size    BIGINT       NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE subject_library_link
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id          BIGINT NOT NULL,
    library_document_id BIGINT NOT NULL,
    linked_at           DATETIME,
    linked_by           VARCHAR(100),

    CONSTRAINT uq_subject_library_link UNIQUE (subject_id, library_document_id),
    CONSTRAINT fk_subject_library_link_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_subject_library_link_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
