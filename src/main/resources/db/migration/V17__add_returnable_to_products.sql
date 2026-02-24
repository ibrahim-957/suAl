ALTER TABLE products
    ADD COLUMN returnable BOOLEAN;

UPDATE products
SET returnable = FALSE
WHERE returnable IS NULL;

ALTER TABLE products
    ALTER COLUMN returnable SET NOT NULL;