ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_number_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_phone_number;
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email;

ALTER TABLE users
    ADD CONSTRAINT uq_users_phone_role UNIQUE (phone_number, role);

ALTER TABLE users
    ADD CONSTRAINT uq_users_email_role UNIQUE (email, role);

CREATE TABLE otp_codes
(
    id           BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20)  NOT NULL,
    code         VARCHAR(6)   NOT NULL,
    expires_at   TIMESTAMP    NOT NULL,
    is_used      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_phone ON otp_codes (phone_number);