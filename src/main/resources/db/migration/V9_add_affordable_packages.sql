ALTER TABLE orders
    ADD COLUMN package_order_id                      BIGINT,
    ADD COLUMN is_package_order                      BOOLEAN,
    ADD COLUMN delivery_number                       INT,
    ADD COLUMN old_containers_to_collect             INT,
    ADD COLUMN expected_deposit_refunded_at_creation DECIMAL(10, 2),
    ADD COLUMN actual_deposit_refunded_at_completion DECIMAL(10, 2);

CREATE INDEX idx_orders_package_order ON orders (package_order_id);
CREATE INDEX idx_orders_is_package ON orders (is_package_order);


CREATE TABLE affordable_packages
(
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT REFERENCES companies (id),
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    total_price DECIMAL(10, 2) NOT NULL,
    is_active   BOOLEAN,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE TABLE affordable_package_products
(
    id         BIGSERIAL PRIMARY KEY,
    package_id BIGINT REFERENCES affordable_packages (id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products (id),
    quantity   INT NOT NULL,
    created_at TIMESTAMP
);


CREATE TABLE customer_package_orders
(
    id                             BIGSERIAL PRIMARY KEY,
    customer_id                    BIGINT REFERENCES customers (id),
    package_id                     BIGINT REFERENCES affordable_packages (id),
    order_number                   VARCHAR(50) UNIQUE NOT NULL,
    frequency                      INT                NOT NULL CHECK (frequency BETWEEN 1 AND 4),

    package_product_price          DECIMAL(10, 2)     NOT NULL,

    total_containers_in_package    INT                NOT NULL,
    old_containers_to_collect      INT,
    total_deposit_charged          DECIMAL(10, 2)     NOT NULL,
    expected_deposit_refunded      DECIMAL(10, 2)     NOT NULL,
    actual_deposit_refunded        DECIMAL(10, 2),
    net_deposit                    DECIMAL(10, 2)     NOT NULL,

    -- Total
    total_price                    DECIMAL(10, 2)     NOT NULL,

    -- Payment
    payment_method                 VARCHAR(20)        NOT NULL,
    payment_status                 VARCHAR(20)        NOT NULL,
    payment_id                     BIGINT REFERENCES payments (id),
    amount_collected_at_delivery_1 DECIMAL(10, 2),

    -- Status
    order_status                   VARCHAR(20)        NOT NULL,
    order_month                    VARCHAR(7)         NOT NULL,
    auto_renew                     BOOLEAN,

    -- Timestamps
    cancelled_at                   TIMESTAMP,
    created_at                     TIMESTAMP,
    updated_at                     TIMESTAMP
);


CREATE TABLE package_delivery_distributions
(
    id               BIGSERIAL PRIMARY KEY,
    package_order_id BIGINT REFERENCES customer_package_orders (id) ON DELETE CASCADE,
    delivery_number  INT  NOT NULL CHECK (delivery_number BETWEEN 1 AND 4),
    delivery_date    DATE NOT NULL,
    address_id       BIGINT REFERENCES addresses (id),
    created_at       TIMESTAMP,
    UNIQUE (package_order_id, delivery_number)
);

CREATE TABLE package_delivery_items
(
    id              BIGSERIAL PRIMARY KEY,
    distribution_id BIGINT REFERENCES package_delivery_distributions (id) ON DELETE CASCADE,
    product_id      BIGINT REFERENCES products (id),
    quantity        INT NOT NULL,
    created_at      TIMESTAMP
);


ALTER TABLE orders
    ADD CONSTRAINT fk_orders_package_order
        FOREIGN KEY (package_order_id) REFERENCES customer_package_orders (id) ON DELETE SET NULL;