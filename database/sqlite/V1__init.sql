CREATE TABLE translate
(
    lang_key TEXT NOT NULL,
    lang     TEXT NOT NULL,
    value    TEXT,

    PRIMARY KEY (lang_key, lang)
);