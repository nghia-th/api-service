-- Extends the textbook library (2026-09-06, per the user's explicit choice to extend rather than
-- create a separate feature) so a document can be a general "mon hoc" (e.g. "Lap trinh Python")
-- with NO grade/curriculum attached, and can carry MULTIPLE files (PDF or PowerPoint slides)
-- instead of exactly one. grade/curriculum become optional; file_path/file_size move out of
-- library_document into the new child table library_document_file (one document -> many files).
ALTER TABLE library_document
    ALTER COLUMN grade DROP NOT NULL;
ALTER TABLE library_document
    ALTER COLUMN curriculum DROP NOT NULL;
ALTER TABLE library_document
    DROP COLUMN file_path;
ALTER TABLE library_document
    DROP COLUMN file_size;

CREATE TABLE library_document_file
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    library_document_id  BIGINT       NOT NULL,
    original_name        VARCHAR(255) NOT NULL,
    file_path            VARCHAR(255) NOT NULL,
    file_size            BIGINT       NOT NULL,
    content_type         VARCHAR(100) NOT NULL,
    uploaded_at          TIMESTAMP,
    uploaded_by          VARCHAR(100),

    CONSTRAINT fk_library_document_file_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
);
CREATE INDEX idx_library_document_file_document ON library_document_file (library_document_id);
