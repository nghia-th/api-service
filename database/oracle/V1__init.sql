CREATE TABLE translate
(
    lang_key VARCHAR2(255) NOT NULL,
    lang     VARCHAR2(20)  NOT NULL,
    value    CLOB,

    CONSTRAINT pk_translate PRIMARY KEY (lang_key, lang)
);
