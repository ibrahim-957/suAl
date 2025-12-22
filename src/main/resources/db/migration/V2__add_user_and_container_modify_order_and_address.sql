CREATE SEQUENCE IF NOT EXISTS order_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE users
(
    id           BIGSERIAL PRIMARY KEY,
    first_name   VARCHAR(255),
    last_name    VARCHAR(255),
    phone_number VARCHAR(50) NOT NULL UNIQUE,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE user_containers
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    product_id BIGINT    NOT NULL,
    quantity   INT       NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_user_containers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_containers_product FOREIGN KEY (product_id) REFERENCES products (id)
);

DELETE FROM orders;

ALTER TABLE orders
    DROP COLUMN customer_name,
    DROP COLUMN phone_number;

ALTER TABLE orders
    ADD COLUMN user_id BIGINT;

ALTER TABLE orders
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id);

DELETE FROM addresses;

ALTER TABLE addresses
    ADD COLUMN user_id BIGINT;

ALTER TABLE addresses
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE addresses
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE addresses
    ADD CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE addresses
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE addresses
    ALTER COLUMN updated_at DROP DEFAULT;