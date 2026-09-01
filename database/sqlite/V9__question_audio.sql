ALTER TABLE question
    ADD COLUMN audio_path TEXT;
ALTER TABLE question
    ADD COLUMN hide_content_in_test BOOLEAN NOT NULL DEFAULT 0;
