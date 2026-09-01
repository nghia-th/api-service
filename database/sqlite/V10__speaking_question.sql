ALTER TABLE question
    ADD COLUMN question_type TEXT NOT NULL DEFAULT 'MULTIPLE_CHOICE';
ALTER TABLE attempt_answer
    ADD COLUMN answer_audio_path TEXT;
ALTER TABLE attempt_answer
    ADD COLUMN parent_marked_correct BOOLEAN;
