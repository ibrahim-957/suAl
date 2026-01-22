ALTER TABLE order_campaign_bonuses
    ADD COLUMN bonus_value NUMERIC(10,2);

UPDATE order_campaign_bonuses
SET bonus_value = 0.00
WHERE bonus_value IS NULL;

ALTER TABLE order_campaign_bonuses
    ALTER COLUMN bonus_value SET NOT NULL;
