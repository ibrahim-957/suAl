CREATE TABLE container_reservations
(
    id                BIGSERIAL PRIMARY KEY,
    customer_id       BIGINT    NOT NULL,
    order_id          BIGINT,
    product_id        BIGINT    NOT NULL,
    quantity_reserved INTEGER   NOT NULL CHECK (quantity_reserved > 0),
    reserved_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at        TIMESTAMP,
    released          BOOLEAN   NOT NULL DEFAULT FALSE,
    released_at       TIMESTAMP,
    CONSTRAINT fk_container_reservation_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_container_reservation_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_container_reservation_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_released_at_when_released
        CHECK (
            (released = TRUE AND released_at IS NOT NULL) OR
            (released = FALSE AND released_at IS NULL)
            )
);