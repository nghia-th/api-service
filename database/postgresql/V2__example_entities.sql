CREATE TABLE category
(
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code   VARCHAR(50),
    name   VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE stock_level
(
    warehouse_code VARCHAR(50)  NOT NULL,
    sku            VARCHAR(50)  NOT NULL,
    quantity       INTEGER      NOT NULL DEFAULT 0,
    unit_price     NUMERIC(18, 2),

    PRIMARY KEY (warehouse_code, sku)
);

CREATE TABLE tag
(
    id       VARCHAR(50) PRIMARY KEY,
    label    VARCHAR(255),
    priority INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE article
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title        VARCHAR(255),
    content      TEXT,
    published_at TIMESTAMP,
    views        BIGINT  NOT NULL DEFAULT 0,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
