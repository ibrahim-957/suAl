ALTER TABLE payments
    RENAME COLUMN amount TO amount_in_coins;

ALTER TABLE payments
    RENAME COLUMN fee TO fee_in_coins;

ALTER TABLE payments
    RENAME COLUMN refund_amount TO refund_amount_in_coins;

ALTER TABLE payments
    RENAME COLUMN redirect_url TO gateway_payment_url;

ALTER TABLE payments
    RENAME COLUMN raw_callback TO raw_callback_response;



ALTER TABLE payments
    ALTER COLUMN amount_in_coins TYPE BIGINT
        USING (amount_in_coins * 100);

ALTER TABLE payments
    ALTER COLUMN fee_in_coins TYPE BIGINT
        USING (fee_in_coins * 100);

ALTER TABLE payments
    ALTER COLUMN refund_amount_in_coins TYPE BIGINT
        USING (refund_amount_in_coins * 100);



ALTER TABLE payments
    ADD COLUMN rrn VARCHAR(100),
    ADD COLUMN approval_code VARCHAR(100),
    ADD COLUMN masked_pan VARCHAR(50),
    ADD COLUMN card_token VARCHAR(100),
    ADD COLUMN card_issuer VARCHAR(100),
    ADD COLUMN three_ds_status VARCHAR(50),

    ADD COLUMN raw_create_response TEXT,
    ADD COLUMN raw_status_response TEXT,

    ADD COLUMN gateway_response_code VARCHAR(100),
    ADD COLUMN gateway_message TEXT,

    ADD COLUMN payment_datetime TIMESTAMP;


ALTER TABLE payments
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
