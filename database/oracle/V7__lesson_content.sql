ALTER TABLE lesson
    ADD (summary       CLOB,
         content       CLOB,
         textbook_page NUMBER,
         image_path    VARCHAR2(255));
