ALTER TABLE categories DROP COLUMN category_type;
ALTER TABLE categories ADD COLUMN name VARCHAR(100);

UPDATE categories SET name = 'Uncategorized' WHERE name IS NULL;

ALTER TABLE categories ALTER COLUMN name SET NOT NULL;
ALTER TABLE categories ADD CONSTRAINT uq_category_name UNIQUE (name);

CREATE TABLE product_sizes (
                               id BIGSERIAL PRIMARY KEY,
                               label VARCHAR(50) NOT NULL,
                               is_active BOOLEAN NOT NULL DEFAULT TRUE,
                               created_at TIMESTAMP NOT NULL,
                               updated_at TIMESTAMP NOT NULL,
                               CONSTRAINT uq_size_label UNIQUE (label)
);

INSERT INTO product_sizes (label, is_active, created_at, updated_at)
SELECT DISTINCT size, TRUE, NOW(), NOW() FROM products WHERE size IS NOT NULL AND size <> '';

ALTER TABLE products ADD COLUMN size_id BIGINT;
UPDATE products p SET size_id = ps.id FROM product_sizes ps WHERE ps.label = p.size;
ALTER TABLE products ALTER COLUMN size_id SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT fk_product_size FOREIGN KEY (size_id) REFERENCES product_sizes(id);
ALTER TABLE products DROP COLUMN size;

ALTER TABLE products ADD COLUMN sell_price NUMERIC(10,2);
UPDATE products p SET sell_price = (
    SELECT pr.sell_price FROM prices pr WHERE pr.product_id = p.id ORDER BY pr.created_at DESC LIMIT 1
);

ALTER TABLE prices DROP COLUMN sell_price;
ALTER TABLE prices ADD COLUMN source_reference VARCHAR(255);
ALTER TABLE products DROP COLUMN order_count;