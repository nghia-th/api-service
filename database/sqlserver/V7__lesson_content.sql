ALTER TABLE lesson
    ADD summary NVARCHAR(MAX),
        content NVARCHAR(MAX),
        textbook_page INT,
        image_path NVARCHAR(255);
