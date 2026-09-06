-- See postgresql/V3__library_document_multi_file.sql for the feature description.
ALTER TABLE library_document
    MODIFY COLUMN grade INT NULL;
ALTER TABLE library_document
    MODIFY COLUMN curriculum VARCHAR(100) NULL;
ALTER TABLE library_document
    DROP COLUMN file_path;
ALTER TABLE library_document
    DROP COLUMN file_size;

CREATE TABLE library_document_file
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    library_document_id  BIGINT       NOT NULL,
    original_name        VARCHAR(255) NOT NULL,
    file_path            VARCHAR(255) NOT NULL,
    file_size            BIGINT       NOT NULL,
    content_type         VARCHAR(100) NOT NULL,
    uploaded_at          DATETIME,
    uploaded_by          VARCHAR(100),

    CONSTRAINT fk_library_document_file_document FOREIGN KEY (library_document_id) REFERENCES library_document (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX idx_library_document_file_document ON library_document_file (library_document_id);
