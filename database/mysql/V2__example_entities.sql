CREATE TABLE category
(
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    code   VARCHAR(50),
    name   VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE stock_level
(
    warehouse_code VARCHAR(50)  NOT NULL,
    sku            VARCHAR(50)  NOT NULL,
    quantity       INT          NOT NULL DEFAULT 0,
    unit_price     DECIMAL(18, 2),

    PRIMARY KEY (warehouse_code, sku)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE tag
(
    id       VARCHAR(50) PRIMARY KEY,
    label    VARCHAR(255),
    priority INT NOT NULL DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE article
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(255),
    content      TEXT,
    published_at DATETIME,
    views        BIGINT  NOT NULL DEFAULT 0,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
