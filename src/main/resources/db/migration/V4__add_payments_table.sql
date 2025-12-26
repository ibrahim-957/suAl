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