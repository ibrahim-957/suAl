ALTER TABLE device_tokens
    ADD COLUMN receiver_type VARCHAR(50);

CREATE INDEX idx_device_tokens_receiver_type ON device_tokens (receiver_type);