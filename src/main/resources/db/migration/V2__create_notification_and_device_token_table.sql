CREATE TABLE notifications
(
    id                BIGSERIAL PRIMARY KEY,
    receiver_type     VARCHAR(20)  NOT NULL,
    receiver_id       BIGINT       NOT NULL,
    notification_type VARCHAR(20)  NOT NULL,
    title             VARCHAR(255) NOT NULL,
    message           VARCHAR(500) NOT NULL,
    reference_id      BIGINT,
    is_read           BOOLEAN      NOT NULL,
    push_sent         BOOLEAN      NOT NULL,
    created_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_notification_receiver
    ON notifications (receiver_type, receiver_id);

CREATE TABLE device_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    receiver_id BIGINT       NOT NULL,
    fcm_token   VARCHAR(255) NOT NULL UNIQUE,
    device_type VARCHAR(20)  NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_device_receiver
    ON device_tokens (receiver_id);