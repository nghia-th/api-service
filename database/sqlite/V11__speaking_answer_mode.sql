ALTER TABLE question
    ADD COLUMN answer_mode TEXT NOT NULL DEFAULT 'AUDIO';
ALTER TABLE question
    ADD COLUMN reference_answer TEXT;
ALTER TABLE attempt_answer
    ADD COLUMN answer_text TEXT;
