CREATE TABLE category
(
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    code   TEXT,
    name   TEXT,
    active BOOLEAN NOT NULL DEFAULT 1
);

CREATE TABLE stock_level
(
    warehouse_code TEXT    NOT NULL,
    sku            TEXT    NOT NULL,
    quantity       INTEGER NOT NULL DEFAULT 0,
    unit_price     DECIMAL(18, 2),

    PRIMARY KEY (warehouse_code, sku)
);

CREATE TABLE tag
(
    id       TEXT PRIMARY KEY,
    label    TEXT,
    priority INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE article
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    title        TEXT,
    content      TEXT,
    published_at DATETIME,
    views        INTEGER NOT NULL DEFAULT 0,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT 0
);
