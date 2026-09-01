ALTER TABLE question
    ADD answer_mode NVARCHAR(20) NOT NULL DEFAULT 'AUDIO',
        reference_answer NVARCHAR(MAX);
ALTER TABLE attempt_answer
    ADD answer_text NVARCHAR(MAX);
