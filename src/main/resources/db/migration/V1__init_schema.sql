CREATE SEQUENCE IF NOT EXISTS order_number_seq START WITH 1 INCREMENT BY 1;


CREATE TABLE users
(
    id           BIGSERIAL PRIMARY KEY,
    first_name   VARCHAR(255),
    last_name    VARCHAR(255),
    phone_number VARCHAR(50) NOT NULL UNIQUE,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL
);


CREATE TABLE categories
(
    id            BIGSERIAL PRIMARY KEY,
    category_type VARCHAR(50) NOT NULL UNIQUE,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL
);


CREATE TABLE companies
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL UNIQUE,
    description    TEXT,
    company_status VARCHAR(50)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);


CREATE TABLE products
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)   NOT NULL,
    description     TEXT,
    image_url       VARCHAR(500),

    company_id      BIGINT         NOT NULL,
    category_id     BIGINT         NOT NULL,

    size            VARCHAR(50),

    deposit_amount  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    product_status  VARCHAR(50)    NOT NULL,

    order_count     BIGINT         NOT NULL DEFAULT 0,

    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL,

    CONSTRAINT uk_products_company_name UNIQUE (company_id, name),
    CONSTRAINT fk_products_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE product_mineral_composition
(
    product_id    BIGINT        NOT NULL,
    mineral_name  VARCHAR(100)  NOT NULL,
    mineral_value VARCHAR(100)  NOT NULL,

    CONSTRAINT pk_product_mineral PRIMARY KEY (product_id, mineral_name),
    CONSTRAINT fk_mineral_product FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE CASCADE
);


CREATE TABLE prices
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT         NOT NULL,
    buy_price  NUMERIC(10, 2) NOT NULL,
    sell_price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP      NOT NULL,

    CONSTRAINT fk_prices_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);


CREATE TABLE operators
(
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone_number    VARCHAR(50),
    operator_status VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);


CREATE TABLE drivers
(
    id            BIGSERIAL PRIMARY KEY,
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) UNIQUE,
    phone_number  VARCHAR(50)  NOT NULL UNIQUE,
    driver_status VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);


CREATE TABLE cars
(
    id           BIGSERIAL PRIMARY KEY,
    driver_id    BIGINT      NOT NULL,
    brand        VARCHAR(100),
    model        VARCHAR(100),
    plate_number VARCHAR(50) NOT NULL UNIQUE,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,

    CONSTRAINT fk_cars_driver FOREIGN KEY (driver_id) REFERENCES drivers (id)
);


CREATE TABLE addresses
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT    NOT NULL,
    description      TEXT      NOT NULL,
    city             VARCHAR(100),
    street           VARCHAR(255),
    building_number  VARCHAR(50),
    apartment_number VARCHAR(50),
    postal_code      VARCHAR(20),
    latitude         NUMERIC(10, 8),
    longitude        NUMERIC(11, 8),
    is_active        BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,

    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
);


CREATE TABLE promos
(
    id               BIGSERIAL PRIMARY KEY,
    promo_code       VARCHAR(50)    NOT NULL UNIQUE,
    description      TEXT,
    discount_type    VARCHAR(50)    NOT NULL,
    discount_value   NUMERIC(10, 2) NOT NULL,
    min_order_amount NUMERIC(10, 2) DEFAULT 0,
    max_discount     NUMERIC(10, 2),
    promo_status     VARCHAR(50)    NOT NULL,
    valid_from       DATE,
    valid_to         DATE,

    max_uses_per_user  INTEGER,
    max_total_uses     INTEGER,
    current_total_uses INTEGER     NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


CREATE TABLE campaigns
(
    id                             BIGSERIAL PRIMARY KEY,
    name                           VARCHAR(255) NOT NULL,
    description                    TEXT,
    campaign_code                  VARCHAR(50) UNIQUE,
    campaign_type                  VARCHAR(50) NOT NULL,

    buy_product_id                 BIGINT NOT NULL,
    buy_quantity                   INTEGER NOT NULL,
    free_product_id                BIGINT NOT NULL,
    free_quantity                  INTEGER NOT NULL,

    first_order_only               BOOLEAN,
    mind_days_since_registration   INT,
    requires_promo_absence         BOOLEAN,

    max_uses_per_user              INTEGER,
    max_total_uses                 INTEGER,
    current_total_uses             INTEGER NOT NULL,

    campaign_status VARCHAR(50) NOT NULL,
    valid_from      DATE,
    valid_to        DATE,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,

    CONSTRAINT fk_campaign_buy_product FOREIGN KEY (buy_product_id) REFERENCES products (id),
    CONSTRAINT fk_campaign_free_product FOREIGN KEY (free_product_id) REFERENCES products (id)
);


CREATE TABLE warehouses
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL UNIQUE,
    warehouse_status VARCHAR(50)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);


CREATE TABLE warehouse_stocks
(
    id                  BIGSERIAL PRIMARY KEY,
    warehouse_id        BIGINT NOT NULL,
    product_id          BIGINT NOT NULL,

    full_count          INTEGER NOT NULL,
    empty_count         INTEGER NOT NULL,
    damaged_count       INTEGER NOT NULL,
    minimum_stock_alert INTEGER NOT NULL,
    last_restocked      TIMESTAMP,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_warehouse_product UNIQUE (warehouse_id, product_id),
    CONSTRAINT fk_ws_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE,
    CONSTRAINT fk_ws_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);


CREATE TABLE orders
(
    id                     BIGSERIAL PRIMARY KEY,
    order_number           VARCHAR(50)    NOT NULL UNIQUE,

    user_id                BIGINT         NOT NULL,
    operator_id            BIGINT,
    driver_id              BIGINT,
    address_id             BIGINT         NOT NULL,

    total_items            INTEGER        NOT NULL,
    subtotal               NUMERIC(10, 2) NOT NULL,

    promo_id               BIGINT,
    promo_discount         NUMERIC(10, 2),
    campaign_discount      NUMERIC(10, 2),

    total_amount           NUMERIC(10, 2) NOT NULL,

    total_deposit_charged  NUMERIC(10, 2),
    total_deposit_refunded NUMERIC(10, 2),
    net_deposit            NUMERIC(10, 2),

    amount                 NUMERIC(10, 2) NOT NULL,
    delivery_date          DATE,

    order_status           VARCHAR(50) NOT NULL,
    payment_method         VARCHAR(50) NOT NULL,
    payment_status         VARCHAR(50) NOT NULL,
    paid_at                TIMESTAMP,

    empty_bottles_expected  INTEGER,
    empty_bottles_collected INTEGER,

    notes                   TEXT,
    rejection_reason        TEXT,

    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_operator FOREIGN KEY (operator_id) REFERENCES operators (id),
    CONSTRAINT fk_orders_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses (id),
    CONSTRAINT fk_orders_promo FOREIGN KEY (promo_id) REFERENCES promos (id)
);


CREATE TABLE order_details
(
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL,
    product_id          BIGINT NOT NULL,
    company_id          BIGINT NOT NULL,
    category_id         BIGINT NOT NULL,

    price_per_unit      NUMERIC(10, 2) NOT NULL,
    buy_price           NUMERIC(10, 2) NOT NULL,
    count               INTEGER        NOT NULL,
    subtotal            NUMERIC(10, 2) NOT NULL,

    deposit_per_unit    NUMERIC(10, 2) NOT NULL,
    containers_returned INTEGER,
    deposit_charged     NUMERIC(10, 2),
    deposit_refunded    NUMERIC(10, 2),
    deposit             NUMERIC(10, 2),

    line_total          NUMERIC(10, 2) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_od_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_od_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_od_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_od_category FOREIGN KEY (category_id) REFERENCES categories (id)
);


CREATE TABLE promo_code_usages
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    promo_id         BIGINT         NOT NULL,
    order_id         BIGINT         NOT NULL,
    campaign_id      BIGINT,
    discount_applied NUMERIC(10, 2) NOT NULL,
    used_at          TIMESTAMP      NOT NULL,

    CONSTRAINT fk_pcu_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_pcu_promo FOREIGN KEY (promo_id) REFERENCES promos (id),
    CONSTRAINT fk_pcu_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_pcu_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
);

CREATE TABLE campaign_usages
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    campaign_id     BIGINT NOT NULL,
    order_id        BIGINT NOT NULL,
    buy_product_id  BIGINT NOT NULL,
    buy_quantity    INTEGER NOT NULL,
    free_product_id BIGINT NOT NULL,
    free_quantity   INTEGER NOT NULL,
    bonus_value     NUMERIC(10, 2) NOT NULL,
    used_at         TIMESTAMP      NOT NULL,

    CONSTRAINT fk_cu_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cu_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id),
    CONSTRAINT fk_cu_order FOREIGN KEY (order_id) REFERENCES orders (id)
);


CREATE TABLE payments
(
    id                     BIGSERIAL PRIMARY KEY,
    order_id               BIGINT         NOT NULL,
    reference_id           VARCHAR(100)   NOT NULL UNIQUE,
    amount                 NUMERIC(10, 2) NOT NULL,
    fee                    NUMERIC(10, 2),
    currency_code          VARCHAR(50)    NOT NULL,
    transaction_type       VARCHAR(50)    NOT NULL,
    payment_method         VARCHAR(50)    NOT NULL,
    payment_provider       VARCHAR(50)    NOT NULL,
    payment_status         VARCHAR(50)    NOT NULL,
    gateway_transaction_id VARCHAR(100),
    redirect_url           TEXT,
    raw_callback           TEXT,
    failure_reason         TEXT,
    gateway_status_code    VARCHAR(100),
    refund_amount          NUMERIC(10, 2),
    refunded_at            TIMESTAMP,
    paid_at                TIMESTAMP,
    created_at             TIMESTAMP,
    updated_at             TIMESTAMP,

    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);