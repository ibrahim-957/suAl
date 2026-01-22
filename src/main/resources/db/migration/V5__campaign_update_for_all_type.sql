ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS bonus_amount DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS bonus_percentage DECIMAL(5, 2);

ALTER TABLE campaigns
    ALTER COLUMN buy_product_id DROP NOT NULL,
    ALTER COLUMN buy_quantity DROP NOT NULL,
    ALTER COLUMN free_product_id DROP NOT NULL,
    ALTER COLUMN free_quantity DROP NOT NULL;

ALTER TABLE order_campaign_bonuses
    ADD COLUMN IF NOT EXISTS bonus_type VARCHAR(50);

ALTER TABLE order_campaign_bonuses
    ALTER COLUMN product_id DROP NOT NULL;

UPDATE order_campaign_bonuses
SET bonus_type = 'FREE_PRODUCT'
WHERE bonus_type IS NULL AND product_id IS NOT NULL;