CREATE TABLE types
(
    id             BIGSERIAL PRIMARY KEY,
    container_type VARCHAR(50) NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories
(
    id            BIGSERIAL PRIMARY KEY,
    category_type VARCHAR(50) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE companies
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    company_status VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255)   NOT NULL,
    company_id     BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    size           VARCHAR(50),
    deposit_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    product_status VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE prices
(
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT         NOT NULL,
    company_id  BIGINT         NOT NULL,
    category_id BIGINT         NOT NULL,
    buy_price   NUMERIC(10, 2) NOT NULL,
    sell_price  NUMERIC(10, 2) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prices_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_prices_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_prices_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE operators
(
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone_number    VARCHAR(50),
    operator_status VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE drivers
(
    id            BIGSERIAL PRIMARY KEY,
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255),
    phone_number  VARCHAR(50)  NOT NULL,
    driver_status VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cars
(
    id           BIGSERIAL PRIMARY KEY,
    driver_id    BIGINT      NOT NULL,
    brand        VARCHAR(100),
    model        VARCHAR(100),
    plate_number VARCHAR(50) NOT NULL UNIQUE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cars_driver FOREIGN KEY (driver_id) REFERENCES drivers (id)
);

CREATE TABLE addresses
(
    id               BIGSERIAL PRIMARY KEY,
    description      TEXT NOT NULL,
    city             VARCHAR(100),
    street           VARCHAR(255),
    building_number  VARCHAR(50),
    apartment_number VARCHAR(50),
    postal_code      VARCHAR(20),
    latitude         NUMERIC(10, 8),
    longitude        NUMERIC(11, 8),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE promos
(
    id               BIGSERIAL PRIMARY KEY,
    promo_code       VARCHAR(50)    NOT NULL UNIQUE,
    description      TEXT,
    discount_type    VARCHAR(50)    NOT NULL,
    discount_value   NUMERIC(10, 2) NOT NULL,
    min_order_amount NUMERIC(10, 2)          DEFAULT 0,
    max_discount     NUMERIC(10, 2),
    promo_status     VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    valid_from       DATE,
    valid_to         DATE,
    created_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE campaigns
(
    id              BIGSERIAL PRIMARY KEY,
    campaign_id     VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    buy_product_id  BIGINT       NOT NULL,
    buy_quantity    INTEGER      NOT NULL,
    free_product_id BIGINT       NOT NULL,
    free_quantity   INTEGER      NOT NULL,
    campaign_status VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    valid_from      DATE,
    valid_to        DATE,
    created_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_campaigns_buy_product FOREIGN KEY (buy_product_id) REFERENCES products (id),
    CONSTRAINT fk_campaigns_free_product FOREIGN KEY (free_product_id) REFERENCES products (id)
);

CREATE TABLE warehouses
(
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL UNIQUE,
    warehouse_status  VARCHAR(50) DEFAULT 'ACTIVE',
    created_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE warehouse_stocks
(
    id                  BIGSERIAL PRIMARY KEY,
    warehouse_id        BIGINT  NOT NULL,
    product_id          BIGINT  NOT NULL,
    company_id          BIGINT  NOT NULL,
    category_id         BIGINT  NOT NULL,
    full_count          INTEGER DEFAULT 0,
    empty_count         INTEGER DEFAULT 0,
    damaged_count       INTEGER DEFAULT 0,
    minimum_stock_alert INTEGER DEFAULT 10,
    last_restocked      TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_warehouse_stocks_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE,
    CONSTRAINT fk_warehouse_stocks_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_warehouse_stocks_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_warehouse_stocks_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT uk_warehouse_product UNIQUE (warehouse_id, product_id)
);

CREATE TABLE orders
(
    id                     BIGSERIAL PRIMARY KEY,
    order_number           VARCHAR(50)    NOT NULL UNIQUE,

    customer_name          VARCHAR(255),
    phone_number           VARCHAR(50),

    operator_id            BIGINT,
    driver_id              BIGINT,
    address_id             BIGINT         NOT NULL,

    count                  INTEGER        NOT NULL,

    subtotal               NUMERIC(10, 2) NOT NULL,
    promo_id               BIGINT,
    promo_discount         NUMERIC(10, 2) DEFAULT 0,
    campaign_discount      NUMERIC(10, 2) DEFAULT 0,
    total_amount           NUMERIC(10, 2) NOT NULL,

    total_deposit_charged  NUMERIC(10, 2) DEFAULT 0,
    total_deposit_refunded NUMERIC(10, 2) DEFAULT 0,
    net_deposit            NUMERIC(10, 2) DEFAULT 0,

    amount                 NUMERIC(10, 2) NOT NULL,

    delivery_date          DATE,

    order_status           VARCHAR(50)    DEFAULT 'PENDING',
    payment_method         VARCHAR(50)    DEFAULT 'CASH',
    payment_status         VARCHAR(50)    DEFAULT 'PENDING',
    paid_at                TIMESTAMP,

    empty_bottles          INTEGER        DEFAULT 0,
    notes                  TEXT,

    created_at             TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_operator FOREIGN KEY (operator_id) REFERENCES operators (id),
    CONSTRAINT fk_orders_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses (id),
    CONSTRAINT fk_orders_promo FOREIGN KEY (promo_id) REFERENCES promos (id)
);

CREATE TABLE order_details
(
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT         NOT NULL,
    product_id          BIGINT         NOT NULL,
    company_id          BIGINT         NOT NULL,
    category_id         BIGINT         NOT NULL,

    price_per_unit      NUMERIC(10, 2) NOT NULL,
    buy_price           NUMERIC(10, 2) NOT NULL,
    count               INTEGER        NOT NULL,
    subtotal            NUMERIC(10, 2) NOT NULL,

    deposit_per_unit    NUMERIC(10, 2) NOT NULL,
    containers_returned INTEGER        DEFAULT 0,
    deposit_charged     NUMERIC(10, 2) DEFAULT 0,
    deposit_refunded    NUMERIC(10, 2) DEFAULT 0,
    deposit             NUMERIC(10, 2) DEFAULT 0,

    line_total          NUMERIC(10, 2) NOT NULL,

    created_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id) REFERENCES "orders" (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_details_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_order_details_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_order_details_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE order_campaign_bonuses
(
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT         NOT NULL,
    campaign_id    BIGINT         NOT NULL,
    product_id     BIGINT         NOT NULL,
    quantity       INTEGER        NOT NULL,
    original_value NUMERIC(10, 2) NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bonuses_order FOREIGN KEY (order_id) REFERENCES "orders" (id) ON DELETE CASCADE,
    CONSTRAINT fk_bonuses_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id),
    CONSTRAINT fk_bonuses_product FOREIGN KEY (product_id) REFERENCES products (id)
);