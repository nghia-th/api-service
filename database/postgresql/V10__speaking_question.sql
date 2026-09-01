ALTER TABLE question
    ADD COLUMN question_type VARCHAR(20) NOT NULL DEFAULT 'MULTIPLE_CHOICE';
ALTER TABLE attempt_answer
    ADD COLUMN answer_audio_path VARCHAR(255),
    ADD COLUMN parent_marked_correct BOOLEAN;
