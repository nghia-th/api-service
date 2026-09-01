ALTER TABLE question
    ADD COLUMN answer_mode VARCHAR(20) NOT NULL DEFAULT 'AUDIO',
    ADD COLUMN reference_answer TEXT;
ALTER TABLE attempt_answer
    ADD COLUMN answer_text TEXT;
