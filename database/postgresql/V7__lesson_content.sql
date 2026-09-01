ALTER TABLE lesson
    ADD COLUMN summary TEXT,
    ADD COLUMN content TEXT,
    ADD COLUMN textbook_page INTEGER,
    ADD COLUMN image_path VARCHAR(255);
