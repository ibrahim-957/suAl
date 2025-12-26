ALTER TABLE user_containers
    ADD CONSTRAINT uk_user_containers_user_product UNIQUE (user_id, product_id);


ALTER TABLE categories
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE categories
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE categories
    ALTER COLUMN category_type SET NOT NULL;

ALTER TABLE categories
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE categories
    ADD CONSTRAINT uk_categories_category_type UNIQUE (category_type);


ALTER TABLE companies
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE companies
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE companies
    ALTER COLUMN company_status DROP DEFAULT;

ALTER TABLE companies
    ADD CONSTRAINT uk_companies_name UNIQUE (name);


ALTER TABLE products
    ALTER COLUMN deposit_amount DROP DEFAULT;

ALTER TABLE products
    ALTER COLUMN product_status DROP DEFAULT;

ALTER TABLE products
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE products
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE products
    ADD CONSTRAINT uk_products_company_name UNIQUE (company_id, name);


ALTER TABLE prices DROP COLUMN company_id;

ALTER TABLE prices DROP COLUMN category_id;

ALTER TABLE prices
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE prices
    ALTER COLUMN updated_at DROP DEFAULT;


ALTER TABLE warehouses
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE warehouses
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE warehouses
    ALTER COLUMN warehouse_status DROP DEFAULT;

ALTER TABLE warehouses
    ADD CONSTRAINT uk_warehouses_name UNIQUE(name);


ALTER TABLE warehouse_stocks DROP COLUMN company_id;

ALTER TABLE warehouse_stocks DROP COLUMN category_id;

ALTER TABLE warehouse_stocks
    ALTER COLUMN full_count DROP DEFAULT;

ALTER TABLE warehouse_stocks
    ALTER COLUMN empty_count DROP DEFAULT;

ALTER TABLE warehouse_stocks
    ALTER COLUMN damaged_count DROP DEFAULT;

ALTER TABLE warehouse_stocks
    ALTER COLUMN minimum_stock_alert DROP DEFAULT;

ALTER TABLE warehouse_stocks
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE warehouse_stocks
    ALTER COLUMN updated_at DROP DEFAULT;


ALTER TABLE operators
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE operators
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE operators
    ALTER COLUMN operator_status DROP DEFAULT;


ALTER TABLE drivers
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE drivers
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE drivers
    ALTER COLUMN driver_status DROP DEFAULT;

ALTER TABLE drivers
    ADD CONSTRAINT uk_drivers_email UNIQUE(email);

ALTER TABLE drivers
    ADD CONSTRAINT uk_drivers_phone UNIQUE(phone_number);


ALTER TABLE cars
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE cars
    ALTER COLUMN updated_at DROP DEFAULT;


ALTER TABLE campaigns
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE campaigns
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE campaigns
    ALTER COLUMN campaign_status DROP DEFAULT;


ALTER TABLE order_campaign_bonuses
    ALTER COLUMN created_at DROP DEFAULT;


ALTER TABLE promos
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE promos
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE promos
    ALTER COLUMN promo_status DROP DEFAULT;


ALTER TABLE orders
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE orders
    ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE orders
    ALTER COLUMN order_status DROP DEFAULT;

ALTER TABLE orders
    ALTER COLUMN payment_method DROP DEFAULT;

ALTER TABLE orders
    ALTER COLUMN payment_status DROP DEFAULT;


ALTER TABLE order_details
    ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE order_details
    ADD COLUMN updated_at TIMESTAMP;


DROP TABLE IF EXISTS types;


