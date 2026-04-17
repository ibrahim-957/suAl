ALTER TABLE orders
    ADD COLUMN stock_reservation_type VARCHAR(50) DEFAULT 'NONE',
    ADD COLUMN stock_reserved_at TIMESTAMP,
    ADD COLUMN stock_reservation_expires_at TIMESTAMP;

UPDATE orders
SET stock_reservation_type = 'NONE'
WHERE stock_reservation_type IS NULL;

ALTER TABLE orders
    ALTER COLUMN stock_reservation_type SET NOT NULL;
