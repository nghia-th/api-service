ALTER TABLE question
    ADD audio_path NVARCHAR(255),
        hide_content_in_test BIT NOT NULL DEFAULT 0;
