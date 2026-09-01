ALTER TABLE question
    ADD (question_type VARCHAR2(20) DEFAULT 'MULTIPLE_CHOICE' NOT NULL);
ALTER TABLE attempt_answer
    ADD (answer_audio_path      VARCHAR2(255),
         parent_marked_correct NUMBER(1));
