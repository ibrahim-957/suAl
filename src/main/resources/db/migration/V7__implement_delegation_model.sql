ALTER TABLE addresses DROP CONSTRAINT IF EXISTS fk_addresses_user;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_user;
ALTER TABLE campaign_usages DROP CONSTRAINT IF EXISTS fk_cu_user;
ALTER TABLE promo_usages DROP CONSTRAINT IF EXISTS fk_pcu_user;


-- ==========================================
-- STEP 1: RENAME COLUMNS IN RELATED TABLES FIRST
-- ==========================================

-- Rename user_id to customer_id in related tables BEFORE renaming the users table
ALTER TABLE addresses RENAME COLUMN user_id TO customer_id;
ALTER TABLE orders RENAME COLUMN user_id TO customer_id;
ALTER TABLE campaign_usages RENAME COLUMN user_id TO customer_id;
ALTER TABLE promo_usages RENAME COLUMN user_id TO customer_id;


-- ==========================================
-- STEP 2: DROP UNIQUE CONSTRAINTS FROM users TABLE
-- ==========================================

-- Drop the unique constraints that will conflict with the new users table
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_number_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_number_key1;


-- ==========================================
-- STEP 3: RENAME users TABLE TO customers
-- ==========================================

-- Rename the table
ALTER TABLE users RENAME TO customers;

-- Update sequence name for clarity
ALTER SEQUENCE users_id_seq RENAME TO customers_id_seq;


-- ==========================================
-- STEP 4: CREATE NEW users TABLE
-- ==========================================

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255),
                       password VARCHAR(255),
                       phone_number VARCHAR(50) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       target_id BIGINT,
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT uk_users_phone UNIQUE (phone_number),
                       CONSTRAINT uk_users_email UNIQUE (email)
);


-- ==========================================
-- STEP 5: ADD user_id TO customers
-- ==========================================

ALTER TABLE customers ADD COLUMN user_id BIGINT;


-- ==========================================
-- STEP 6: MIGRATE CUSTOMER DATA TO users
-- ==========================================

-- Insert all customers into users table with CUSTOMER role
INSERT INTO users (phone_number, role, target_id, is_active, created_at, updated_at)
SELECT
    phone_number,
    'CUSTOMER',
    id,
    COALESCE(is_active, TRUE),
    created_at,
    updated_at
FROM customers;

-- Update customers with their corresponding user_id
UPDATE customers c
SET user_id = u.id
FROM users u
WHERE u.target_id = c.id AND u.role = 'CUSTOMER';


-- ==========================================
-- STEP 7: REMOVE phone_number FROM customers
-- ==========================================

ALTER TABLE customers DROP COLUMN phone_number;


-- ==========================================
-- STEP 8: ADD user_id TO drivers
-- ==========================================

ALTER TABLE drivers ADD COLUMN user_id BIGINT;


-- ==========================================
-- STEP 9: MIGRATE DRIVER DATA TO users
-- ==========================================

-- Insert all drivers into users table with DRIVER role
INSERT INTO users (email, phone_number, role, target_id, created_at, updated_at)
SELECT
    email,
    phone_number,
    'DRIVER',
    id,
    created_at,
    updated_at
FROM drivers;

-- Update drivers with their corresponding user_id
UPDATE drivers d
SET user_id = u.id
FROM users u
WHERE u.target_id = d.id AND u.role = 'DRIVER';


-- ==========================================
-- STEP 10: REMOVE email AND phone_number FROM drivers
-- ==========================================

ALTER TABLE drivers
    DROP COLUMN email,
    DROP COLUMN phone_number;


-- ==========================================
-- STEP 11: ADD user_id TO operators
-- ==========================================

ALTER TABLE operators ADD COLUMN user_id BIGINT;


-- ==========================================
-- STEP 12: MIGRATE OPERATOR DATA TO users
-- ==========================================

-- Insert all operators into users table with OPERATOR role
INSERT INTO users (email, phone_number, role, target_id, created_at, updated_at)
SELECT
    email,
    phone_number,
    'OPERATOR',
    id,
    created_at,
    updated_at
FROM operators;

-- Update operators with their corresponding user_id
UPDATE operators o
SET user_id = u.id
FROM users u
WHERE u.target_id = o.id AND u.role = 'OPERATOR';


-- ==========================================
-- STEP 13: REMOVE email AND phone_number FROM operators
-- ==========================================

ALTER TABLE operators
    DROP COLUMN email,
    DROP COLUMN phone_number;


-- ==========================================
-- STEP 14: CREATE customer_containers TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS customer_containers (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   customer_id BIGINT NOT NULL,
                                                   product_id BIGINT NOT NULL,
                                                   quantity INTEGER NOT NULL DEFAULT 0,
                                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                   CONSTRAINT uk_customer_product UNIQUE (customer_id, product_id),
                                                   CONSTRAINT fk_cc_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
                                                   CONSTRAINT fk_cc_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);


-- ==========================================
-- STEP 15: ADD CONSTRAINTS TO user_id COLUMNS
-- ==========================================

-- Add constraints to customers.user_id
ALTER TABLE customers
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT uk_customer_user UNIQUE (user_id),
    ADD CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- Add constraints to drivers.user_id
ALTER TABLE drivers
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT uk_driver_user UNIQUE (user_id),
    ADD CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- Add constraints to operators.user_id
ALTER TABLE operators
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT uk_operator_user UNIQUE (user_id),
    ADD CONSTRAINT fk_operator_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;


-- ==========================================
-- STEP 16: ADD NEW FOREIGN KEY CONSTRAINTS
-- ==========================================

-- Add new foreign key constraints pointing to customers
ALTER TABLE addresses
    ADD CONSTRAINT fk_addresses_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE;

ALTER TABLE campaign_usages
    ADD CONSTRAINT fk_cu_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE;

ALTER TABLE promo_usages
    ADD CONSTRAINT fk_pcu_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE;


-- ==========================================
-- STEP 17: UPDATE campaigns TABLE
-- ==========================================

-- Add new column
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS min_days_since_registration INTEGER;

-- Rename column for consistency
ALTER TABLE campaigns RENAME COLUMN max_uses_per_user TO max_uses_per_customer;


-- ==========================================
-- STEP 18: CREATE INDEXES FOR PERFORMANCE
-- ==========================================

-- Indexes on users table
CREATE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;
CREATE INDEX idx_users_phone ON users (phone_number);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_target_id ON users (target_id);

-- Indexes on profile tables
CREATE INDEX idx_customers_user_id ON customers (user_id);
CREATE INDEX idx_drivers_user_id ON drivers (user_id);
CREATE INDEX idx_operators_user_id ON operators (user_id);

-- Indexes on relationship tables
CREATE INDEX idx_addresses_customer_id ON addresses (customer_id);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_campaign_usages_customer_id ON campaign_usages (customer_id);
CREATE INDEX idx_promo_usages_customer_id ON promo_usages (customer_id);
CREATE INDEX idx_customer_containers_customer_id ON customer_containers (customer_id);


-- ==========================================
-- STEP 19: DATA INTEGRITY VERIFICATION
-- ==========================================

-- Verify all customers have users
DO $$
    DECLARE
        missing_count INTEGER;
    BEGIN
        SELECT COUNT(*) INTO missing_count
        FROM customers c
        WHERE c.user_id IS NULL OR NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = c.user_id
        );

        IF missing_count > 0 THEN
            RAISE EXCEPTION 'Data integrity check failed: % customers without users', missing_count;
        END IF;
    END $$;

-- Verify all drivers have users
DO $$
    DECLARE
        missing_count INTEGER;
    BEGIN
        SELECT COUNT(*) INTO missing_count
        FROM drivers d
        WHERE d.user_id IS NULL OR NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = d.user_id
        );

        IF missing_count > 0 THEN
            RAISE EXCEPTION 'Data integrity check failed: % drivers without users', missing_count;
        END IF;
    END $$;

-- Verify all operators have users
DO $$
    DECLARE
        missing_count INTEGER;
    BEGIN
        SELECT COUNT(*) INTO missing_count
        FROM operators o
        WHERE o.user_id IS NULL OR NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = o.user_id
        );

        IF missing_count > 0 THEN
            RAISE EXCEPTION 'Data integrity check failed: % operators without users', missing_count;
        END IF;
    END $$;

-- Verify all addresses point to valid customers
DO $$
    DECLARE
        orphaned_count INTEGER;
    BEGIN
        SELECT COUNT(*) INTO orphaned_count
        FROM addresses a
        WHERE NOT EXISTS (
            SELECT 1 FROM customers c WHERE c.id = a.customer_id
        );

        IF orphaned_count > 0 THEN
            RAISE EXCEPTION 'Data integrity check failed: % addresses without customers', orphaned_count;
        END IF;
    END $$;

-- Verify all orders point to valid customers
DO $$
    DECLARE
        orphaned_count INTEGER;
    BEGIN
        SELECT COUNT(*) INTO orphaned_count
        FROM orders o
        WHERE NOT EXISTS (
            SELECT 1 FROM customers c WHERE c.id = o.customer_id
        );

        IF orphaned_count > 0 THEN
            RAISE EXCEPTION 'Data integrity check failed: % orders without customers', orphaned_count;
        END IF;
    END $$;