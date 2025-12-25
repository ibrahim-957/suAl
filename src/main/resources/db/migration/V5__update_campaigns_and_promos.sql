ALTER TABLE campaigns
    ADD COLUMN name               VARCHAR(255),
    ADD COLUMN max_uses_per_user  INTEGER,
    ADD COLUMN max_total_uses     INTEGER,
    ADD COLUMN current_total_uses INTEGER;

ALTER TABLE campaigns
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN current_total_uses SET NOT NULL;

ALTER TABLE promos
    ADD COLUMN max_uses_per_user  INTEGER,
    ADD COLUMN max_total_uses     INTEGER,
    ADD COLUMN current_total_uses INTEGER;

UPDATE promos
SET current_total_uses = 0
WHERE current_total_uses IS NULL;

ALTER TABLE promos
    ALTER COLUMN current_total_uses SET NOT NULL;
