ALTER TABLE campaigns
    ADD COLUMN campaign_code                VARCHAR(50) UNIQUE,
    ADD COLUMN campaign_type                VARCHAR(50) NOT NULL,
    ADD COLUMN first_order_only             BOOLEAN,
    ADD COLUMN mind_days_since_registration INT,
    ADD COLUMN requires_promo_absence       BOOLEAN;

ALTER TABLE campaign_usages
    ADD COLUMN bonus_value DECIMAL(10, 2) NOT NULL;

ALTER TABLE campaigns
    DROP COLUMN campaign_id;