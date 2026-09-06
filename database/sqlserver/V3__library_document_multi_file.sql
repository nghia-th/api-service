-- See postgresql/V3__library_document_multi_file.sql for the feature description.
ALTER TABLE library_document ALTER COLUMN grade INT NULL;
ALTER TABLE library_document ALTER COLUMN curriculum NVARCHAR(100) NULL;
ALTER TABLE library_document DROP COLUMN file_path;
ALTER TABLE library_document DROP COLUMN file_size;

CREATE TABLE library_document_file
(
    id                   BIGINT IDENTITY (1,1) PRIMARY KEY,
    library_document_id  BIGINT        NOT NULL,
    original_name        NVARCHAR(255) NOT NULL,
    file_path            NVARCHAR(255) NOT NULL,
    file_size            BIGINT        NOT NULL,
    content_type         NVARCHAR(100) NOT NULL,
    uploaded_at          DATETIME2,
    uploaded_by          NVARCHAR(100),

    CONSTRAINT fk_library_document_file_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
);
CREATE INDEX idx_library_document_file_document ON library_document_file (library_document_id);
