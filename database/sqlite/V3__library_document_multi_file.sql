-- See postgresql/V3__library_document_multi_file.sql for the feature description. SQLite cannot
-- ALTER COLUMN to drop NOT NULL or DROP COLUMN in a way this project relies on elsewhere, so this
-- rebuilds library_document from scratch - same "PRAGMA foreign_keys=OFF -> new table -> copy ->
-- drop old -> rename -> PRAGMA foreign_keys=ON" shape as the earlier subject-shared-across-
-- classrooms migration. subject_library_link's own FK declaration is untouched - it still just
-- names the table "library_document", which keeps existing after the rebuild (same id values).
PRAGMA foreign_keys = OFF;

CREATE TABLE library_document_new
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    grade        INTEGER,
    subject_name TEXT NOT NULL,
    curriculum   TEXT,
    volume       TEXT,
    title        TEXT NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT 0
);

INSERT INTO library_document_new (id, grade, subject_name, curriculum, volume, title, created_at, updated_at, created_by, updated_by, deleted)
SELECT id, grade, subject_name, curriculum, volume, title, created_at, updated_at, created_by, updated_by, deleted
FROM library_document;

CREATE TABLE library_document_file
(
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    library_document_id INTEGER NOT NULL,
    original_name        TEXT    NOT NULL,
    file_path            TEXT    NOT NULL,
    file_size            INTEGER NOT NULL,
    content_type         TEXT    NOT NULL,
    uploaded_at          DATETIME,
    uploaded_by          TEXT,

    CONSTRAINT fk_library_document_file_document FOREIGN KEY (library_document_id) REFERENCES library_document_new (id)
);

-- Carries forward any existing single-file row's PDF (physical file on disk is untouched, same
-- filename - only the DB metadata moves into the new child table) as that document's first file.
INSERT INTO library_document_file (library_document_id, original_name, file_path, file_size, content_type, uploaded_at, uploaded_by)
SELECT id, title, file_path, file_size, 'application/pdf', created_at, created_by
FROM library_document
WHERE file_path IS NOT NULL AND file_path <> '';

DROP TABLE library_document;
ALTER TABLE library_document_new RENAME TO library_document;

CREATE INDEX idx_library_document_file_document ON library_document_file (library_document_id);

UPDATE sqlite_sequence SET seq = (SELECT COALESCE(MAX(id), 0) FROM library_document) WHERE name = 'library_document';

PRAGMA foreign_keys = ON;
