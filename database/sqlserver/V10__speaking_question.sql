ALTER TABLE question
    ADD question_type NVARCHAR(20) NOT NULL DEFAULT 'MULTIPLE_CHOICE';
ALTER TABLE attempt_answer
    ADD answer_audio_path NVARCHAR(255),
        parent_marked_correct BIT;
