-- Textbook library feature (2026-09-05): Admin uploads PDF textbooks organized by
-- Grade -> Subject name -> Curriculum series -> Volume (e.g. "Lop 4 -> Toan tap 1 -> Ket noi tri
-- thuc"). A Parent's own Subject rows can each link to MULTIPLE library documents
-- (subject_library_link is a many-to-many join, see LibraryService's javadoc) - both Parent and
-- Student can then view/download a linked document.
CREATE TABLE library_document
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    grade        INTEGER NOT NULL,
    subject_name TEXT    NOT NULL,
    curriculum   TEXT    NOT NULL,
    volume       TEXT,
    title        TEXT    NOT NULL,
    file_path    TEXT    NOT NULL,
    file_size    INTEGER NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE subject_library_link
(
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id          INTEGER NOT NULL,
    library_document_id INTEGER NOT NULL,
    linked_at           DATETIME,
    linked_by           TEXT,

    CONSTRAINT uq_subject_library_link UNIQUE (subject_id, library_document_id),
    CONSTRAINT fk_subject_library_link_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_subject_library_link_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
);
