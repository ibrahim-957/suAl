CREATE SEQUENCE IF NOT EXISTS order_number_seq START WITH 1 INCREMENT BY 1;


-- =========================
-- USERS
-- =========================
CREATE TABLE users
(
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(255) UNIQUE,
    password     VARCHAR(255),
    phone_number VARCHAR(50) NOT NULL UNIQUE,
    target_id    BIGINT,
    role         VARCHAR(50) NOT NULL,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL
);

-- =========================
-- COMPANIES
-- =========================
CREATE TABLE companies
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL UNIQUE,
    description    TEXT,
    company_status VARCHAR(50)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);

-- =========================
-- CATEGORIES
-- =========================
CREATE TABLE categories
(
    id            BIGSERIAL PRIMARY KEY,
    category_type VARCHAR(50) NOT NULL UNIQUE,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL
);

-- =========================
-- WAREHOUSES
-- =========================
CREATE TABLE warehouses
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL UNIQUE,
    warehouse_status VARCHAR(50)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

-- =========================
-- PRODUCTS
-- =========================
CREATE TABLE products
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255)   NOT NULL,
    description    TEXT           NOT NULL,
    image_url      VARCHAR(255)   NOT NULL,
    company_id     BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    size           VARCHAR(100),
    deposit_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    product_status VARCHAR(50)    NOT NULL,
    order_count    BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    CONSTRAINT uq_product_company_name UNIQUE (company_id, name),
    CONSTRAINT fk_product_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- =========================
-- PRODUCT MINERAL COMPOSITION
-- =========================
CREATE TABLE product_mineral_composition
(
    product_id    BIGINT       NOT NULL,
    mineral_name  VARCHAR(255) NOT NULL,
    mineral_value VARCHAR(255),
    PRIMARY KEY (product_id, mineral_name),
    CONSTRAINT fk_mineral_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- =========================
-- PRICES
-- =========================
CREATE TABLE prices
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT         NOT NULL,
    buy_price  NUMERIC(10, 2) NOT NULL,
    sell_price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP      NOT NULL,
    CONSTRAINT fk_price_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- =========================
-- WAREHOUSE STOCKS
-- =========================
CREATE TABLE warehouse_stocks
(
    id                  BIGSERIAL PRIMARY KEY,
    warehouse_id        BIGINT    NOT NULL,
    product_id          BIGINT    NOT NULL,
    full_count          INT       NOT NULL DEFAULT 0,
    empty_count         INT       NOT NULL DEFAULT 0,
    damaged_count       INT       NOT NULL DEFAULT 0,
    minimum_stock_alert INT                DEFAULT 10,
    last_restocked      TIMESTAMP,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    CONSTRAINT uq_warehouse_product UNIQUE (warehouse_id, product_id),
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- =========================
-- CUSTOMERS
-- =========================
CREATE TABLE customers
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- =========================
-- OPERATORS
-- =========================
CREATE TABLE operators
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL UNIQUE,
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    operator_status VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT fk_operator_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- =========================
-- DRIVERS
-- =========================
CREATE TABLE drivers
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL UNIQUE,
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    driver_status VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- =========================
-- CARS
-- =========================
CREATE TABLE cars
(
    id           BIGSERIAL PRIMARY KEY,
    driver_id    BIGINT      NOT NULL,
    brand        VARCHAR(255),
    model        VARCHAR(255),
    plate_number VARCHAR(50) NOT NULL UNIQUE,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,
    CONSTRAINT fk_car_driver FOREIGN KEY (driver_id) REFERENCES drivers (id)
);

-- =========================
-- PROMOS
-- =========================
CREATE TABLE promos
(
    id                    BIGSERIAL PRIMARY KEY,
    promo_code            VARCHAR(100)   NOT NULL UNIQUE,
    description           TEXT,
    discount_type         VARCHAR(50)    NOT NULL,
    discount_value        NUMERIC(10, 2) NOT NULL,
    min_order_amount      NUMERIC(10, 2)          DEFAULT 0,
    max_discount          NUMERIC(10, 2),
    max_uses_per_customer INT,
    max_total_uses        INT,
    current_total_uses    INT            NOT NULL DEFAULT 0,
    promo_status          VARCHAR(50)    NOT NULL,
    valid_from            DATE,
    valid_to              DATE,
    created_at            TIMESTAMP      NOT NULL,
    updated_at            TIMESTAMP      NOT NULL
);

-- =========================
-- CAMPAIGNS
-- =========================
CREATE TABLE campaigns
(
    id                          BIGSERIAL PRIMARY KEY,
    campaign_code               VARCHAR(100) NOT NULL UNIQUE,
    name                        VARCHAR(255) NOT NULL,
    description                 TEXT,
    image_url                   VARCHAR(255) NOT NULL,
    campaign_type               VARCHAR(50)  NOT NULL,
    buy_product_id              BIGINT,
    buy_quantity                INT,
    free_product_id             BIGINT,
    free_quantity               INT,
    bonus_amount                NUMERIC(10, 2),
    bonus_percentage            NUMERIC(15, 2),
    first_order_only            BOOLEAN               DEFAULT FALSE,
    min_days_since_registration INT,
    requires_promo_absence      BOOLEAN               DEFAULT FALSE,
    max_uses_per_customer       INT,
    max_total_uses              INT,
    current_total_uses          INT          NOT NULL DEFAULT 0,
    campaign_status             VARCHAR(50)  NOT NULL,
    valid_from                  DATE,
    valid_to                    DATE,
    created_at                  TIMESTAMP    NOT NULL,
    updated_at                  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_campaign_buy_product FOREIGN KEY (buy_product_id) REFERENCES products (id),
    CONSTRAINT fk_campaign_free_product FOREIGN KEY (free_product_id) REFERENCES products (id)
);

-- =========================
-- ORDERS
-- =========================
CREATE TABLE orders
(
    id                      BIGSERIAL PRIMARY KEY,
    order_number            VARCHAR(100)   NOT NULL UNIQUE,
    customer_id             BIGINT         NOT NULL,
    operator_id             BIGINT,
    driver_id               BIGINT,
    address_id              BIGINT         NOT NULL,
    total_items             INT            NOT NULL,
    subtotal                NUMERIC(10, 2) NOT NULL,
    promo_id                BIGINT,
    promo_discount          NUMERIC(10, 2)          DEFAULT 0,
    campaign_discount       NUMERIC(10, 2)          DEFAULT 0,
    total_amount            NUMERIC(10, 2) NOT NULL,
    total_deposit_charged   NUMERIC(10, 2)          DEFAULT 0,
    total_deposit_refunded  NUMERIC(10, 2)          DEFAULT 0,
    net_deposit             NUMERIC(10, 2) NOT NULL DEFAULT 0,
    amount                  NUMERIC(10, 2) NOT NULL,
    delivery_date           DATE,
    order_status            VARCHAR(50),
    payment_method          VARCHAR(50),
    payment_status          VARCHAR(50),
    paid_at                 TIMESTAMP,
    empty_bottles_expected  INT                     DEFAULT 0,
    empty_bottles_collected INT,
    notes                   TEXT,
    rejection_reason        TEXT,
    created_at              TIMESTAMP      NOT NULL,
    updated_at              TIMESTAMP      NOT NULL,
    completed_at            TIMESTAMP,
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_order_operator FOREIGN KEY (operator_id) REFERENCES operators (id),
    CONSTRAINT fk_order_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT fk_order_promo FOREIGN KEY (promo_id) REFERENCES promos (id)
);

-- =========================
-- ORDER DETAILS
-- =========================
CREATE TABLE order_details
(
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT         NOT NULL,
    product_id          BIGINT         NOT NULL,
    company_id          BIGINT         NOT NULL,
    category_id         BIGINT         NOT NULL,
    price_per_unit      NUMERIC(10, 2) NOT NULL,
    buy_price           NUMERIC(10, 2) NOT NULL,
    count               INT            NOT NULL,
    subtotal            NUMERIC(10, 2) NOT NULL,
    deposit_per_unit    NUMERIC(10, 2) NOT NULL,
    containers_returned INT            DEFAULT 0,
    deposit_charged     NUMERIC(10, 2) DEFAULT 0,
    deposit_refunded    NUMERIC(10, 2) DEFAULT 0,
    deposit             NUMERIC(10, 2) DEFAULT 0,
    line_total          NUMERIC(10, 2) NOT NULL,
    created_at          TIMESTAMP      NOT NULL,
    updated_at          TIMESTAMP      NOT NULL,
    CONSTRAINT fk_order_detail_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_detail_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_order_detail_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_order_detail_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- =========================
-- PAYMENTS
-- =========================
CREATE TABLE payments
(
    id                     BIGSERIAL PRIMARY KEY,
    order_id               BIGINT       NOT NULL,
    reference_id           VARCHAR(255) NOT NULL UNIQUE,
    amount_in_coins        BIGINT       NOT NULL,
    fee_in_coins           BIGINT,
    currency_code          VARCHAR(10)  NOT NULL,
    transaction_type       VARCHAR(50)  NOT NULL,
    payment_method         VARCHAR(50)  NOT NULL,
    payment_provider       VARCHAR(50)  NOT NULL,
    payment_status         VARCHAR(50)  NOT NULL,
    gateway_payment_url    TEXT,
    gateway_transaction_id VARCHAR(255),
    rrn                    VARCHAR(255),
    approval_code          VARCHAR(255),
    masked_pan             VARCHAR(255),
    card_token             VARCHAR(255),
    card_issuer            VARCHAR(255),
    three_ds_status        VARCHAR(255),
    raw_create_response    TEXT,
    raw_status_response    TEXT,
    raw_callback_response  TEXT,
    gateway_status_code    VARCHAR(255),
    gateway_response_code  VARCHAR(255),
    gateway_message        VARCHAR(255),
    failure_reason         VARCHAR(255),
    refund_amount_in_coins BIGINT,
    refunded_at            TIMESTAMP,
    paid_at                TIMESTAMP,
    payment_datetime       TIMESTAMP,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP    NOT NULL,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- =========================
-- NOTIFICATIONS
-- =========================
CREATE TABLE notifications
(
    id                BIGSERIAL PRIMARY KEY,
    receiver_type     VARCHAR(50)  NOT NULL,
    receiver_id       BIGINT,
    notification_type VARCHAR(50)  NOT NULL,
    title             VARCHAR(255) NOT NULL,
    message           TEXT         NOT NULL,
    reference_id      BIGINT,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    push_sent         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_notification_receiver ON notifications (receiver_type, receiver_id);
CREATE INDEX idx_notification_read ON notifications (is_read);
CREATE INDEX idx_notification_created ON notifications (created_at);

-- =========================
-- DEVICE TOKENS
-- =========================
CREATE TABLE device_tokens
(
    id            BIGSERIAL PRIMARY KEY,
    receiver_id   BIGINT       NOT NULL,
    fcm_token     VARCHAR(255) NOT NULL UNIQUE,
    device_type   VARCHAR(50)  NOT NULL,
    receiver_type VARCHAR(50)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX idx_device_receiver ON device_tokens (receiver_type, receiver_id);
CREATE INDEX idx_device_token ON device_tokens (fcm_token);


-- ======================
-- Table: addresses
-- ======================
CREATE TABLE addresses
(
    id               BIGSERIAL PRIMARY KEY,
    description      TEXT      NOT NULL,
    city             TEXT,
    street           TEXT,
    building_number  TEXT,
    apartment_number TEXT,
    postal_code      TEXT,
    latitude         NUMERIC(10, 8),
    longitude        NUMERIC(11, 8),
    customer_id      BIGINT    NOT NULL,
    is_active        BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_address_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

-- ======================
-- Table: admins
-- ======================
CREATE TABLE admins
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL UNIQUE,
    first_name TEXT,
    last_name  TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- ======================
-- Table: campaign_usages
-- ======================
CREATE TABLE campaign_usages
(
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT         NOT NULL,
    campaign_id     BIGINT         NOT NULL,
    order_id        BIGINT         NOT NULL,
    buy_product_id  BIGINT,
    buy_quantity    INT            NOT NULL,
    free_product_id BIGINT,
    free_quantity   INT            NOT NULL,
    bonus_value     NUMERIC(10, 2) NOT NULL,
    used_at         TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT fk_campaign_usage_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_campaign_usage_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id),
    CONSTRAINT fk_campaign_usage_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_campaign_usage_buy_product FOREIGN KEY (buy_product_id) REFERENCES products (id),
    CONSTRAINT fk_campaign_usage_free_product FOREIGN KEY (free_product_id) REFERENCES products (id)
);

-- ======================
-- Table: customer_containers
-- ======================
CREATE TABLE customer_containers
(
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT    NOT NULL,
    product_id  BIGINT    NOT NULL,
    quantity    INT       NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_customer_container_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_customer_container_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_customer_product UNIQUE (customer_id, product_id)
);

-- ======================
-- Table: order_campaign_bonuses
-- ======================
CREATE TABLE order_campaign_bonuses
(
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT         NOT NULL,
    campaign_id BIGINT         NOT NULL,
    product_id  BIGINT,
    quantity    INT            NOT NULL,
    bonus_value NUMERIC(10, 2) NOT NULL,
    bonus_type  TEXT,
    created_at  TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT fk_order_campaign_bonus_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_campaign_bonus_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id),
    CONSTRAINT fk_order_campaign_bonus_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- ======================
-- Table: promo_usages
-- ======================
CREATE TABLE promo_usages
(
    id               BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT         NOT NULL,
    promo_id         BIGINT         NOT NULL,
    order_id         BIGINT         NOT NULL,
    campaign_id      BIGINT,
    discount_applied NUMERIC(10, 2) NOT NULL,
    used_at          TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT fk_promo_usage_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_promo_usage_promo FOREIGN KEY (promo_id) REFERENCES promos (id),
    CONSTRAINT fk_promo_usage_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_promo_usage_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
);
