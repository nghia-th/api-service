ALTER TABLE question
    ADD COLUMN audio_path VARCHAR(255),
    ADD COLUMN hide_content_in_test BOOLEAN NOT NULL DEFAULT FALSE;
