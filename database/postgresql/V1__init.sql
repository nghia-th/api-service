CREATE TABLE translate
(
    lang_key VARCHAR(255) NOT NULL,
    lang     VARCHAR(20)  NOT NULL,
    value    TEXT,

    PRIMARY KEY (lang_key, lang)
);
