-- Textbook library feature (2026-09-05): Admin uploads PDF textbooks organized by
-- Grade -> Subject name -> Curriculum series -> Volume (e.g. "Lop 4 -> Toan tap 1 -> Ket noi tri
-- thuc"). A Parent's own Subject rows can each link to MULTIPLE library documents
-- (subject_library_link is a many-to-many join, see LibraryService's javadoc) - both Parent and
-- Student can then view/download a linked document.
CREATE TABLE library_document
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    grade        INT           NOT NULL,
    subject_name NVARCHAR(255) NOT NULL,
    curriculum   NVARCHAR(100) NOT NULL,
    volume       NVARCHAR(100),
    title        NVARCHAR(255) NOT NULL,
    file_path    NVARCHAR(255) NOT NULL,
    file_size    BIGINT        NOT NULL,
    created_at   DATETIME2,
    updated_at   DATETIME2,
    created_by   NVARCHAR(100),
    updated_by   NVARCHAR(100),
    deleted      BIT           NOT NULL DEFAULT 0
);

CREATE TABLE subject_library_link
(
    id                  BIGINT IDENTITY (1,1) PRIMARY KEY,
    subject_id          BIGINT NOT NULL,
    library_document_id BIGINT NOT NULL,
    linked_at           DATETIME2,
    linked_by           NVARCHAR(100),

    CONSTRAINT uq_subject_library_link UNIQUE (subject_id, library_document_id),
    CONSTRAINT fk_subject_library_link_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_subject_library_link_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
);
