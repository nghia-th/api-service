ALTER TABLE question
    ADD (audio_path           VARCHAR2(255),
         hide_content_in_test NUMBER(1) DEFAULT 0 NOT NULL);
