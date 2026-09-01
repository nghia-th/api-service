ALTER TABLE question
    ADD (answer_mode      VARCHAR2(20) DEFAULT 'AUDIO' NOT NULL,
         reference_answer CLOB);
ALTER TABLE attempt_answer
    ADD (answer_text CLOB);
