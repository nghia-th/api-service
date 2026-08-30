CREATE TABLE category
(
    id     BIGINT IDENTITY (1,1) PRIMARY KEY,
    code   NVARCHAR(50),
    name   NVARCHAR(255),
    active BIT NOT NULL DEFAULT 1
);

CREATE TABLE stock_level
(
    warehouse_code NVARCHAR(50)   NOT NULL,
    sku            NVARCHAR(50)   NOT NULL,
    quantity       INT            NOT NULL DEFAULT 0,
    unit_price     DECIMAL(18, 2),

    CONSTRAINT PK_stock_level PRIMARY KEY (warehouse_code, sku)
);

CREATE TABLE tag
(
    id       NVARCHAR(50) PRIMARY KEY,
    label    NVARCHAR(255),
    priority INT NOT NULL DEFAULT 0
);

CREATE TABLE article
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    title        NVARCHAR(255),
    content      NVARCHAR(MAX),
    published_at DATETIME2,
    views        BIGINT NOT NULL DEFAULT 0,
    created_at   DATETIME2,
    updated_at   DATETIME2,
    created_by   NVARCHAR(100),
    updated_by   NVARCHAR(100),
    deleted      BIT    NOT NULL DEFAULT 0
);
