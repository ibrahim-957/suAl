DROP TABLE IF EXISTS prices;

CREATE TABLE product_prices
(
    id               BIGSERIAL      NOT NULL,
    product_id       BIGINT         NOT NULL,
    created_by       BIGINT         NOT NULL,
    buy_price        NUMERIC(10, 2) NOT NULL,
    sell_price       NUMERIC(10, 2) NOT NULL,
    discount_percent NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    valid_from       TIMESTAMP      NOT NULL,
    valid_to         TIMESTAMP      NULL,
    source_reference VARCHAR(255)   NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product_prices PRIMARY KEY (id),
    CONSTRAINT fk_product_prices_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_prices_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_product_prices_product ON product_prices (product_id);
CREATE INDEX idx_product_prices_active  ON product_prices (product_id) WHERE valid_to IS NULL;


ALTER TABLE products DROP COLUMN IF EXISTS sell_price;


ALTER TABLE users DROP COLUMN IF EXISTS target_id;


DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_container_reservations_product'
              AND table_name      = 'container_reservations'
        ) THEN
            ALTER TABLE container_reservations
                ADD CONSTRAINT fk_container_reservations_product
                    FOREIGN KEY (product_id) REFERENCES products (id);
        END IF;
    END $$;



CREATE TABLE stock_movements
(
    id             BIGSERIAL      NOT NULL,
    product_id     BIGINT         NOT NULL,
    warehouse_id   BIGINT         NOT NULL,
    created_by     BIGINT         NOT NULL,
    movement_type  VARCHAR(50)    NOT NULL,
    reference_type VARCHAR(50)    NOT NULL,
    reference_id   BIGINT         NOT NULL,
    quantity       INTEGER        NOT NULL,
    unit_price     NUMERIC(10, 2) NULL,
    notes          TEXT           NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT chk_stock_movements_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_stock_movements_movement_type
        CHECK (movement_type IN (
                                 'PURCHASE',
                                 'SALE',
                                 'TRANSFER_IN',
                                 'TRANSFER_OUT',
                                 'RETURN_FROM_CUSTOMER',
                                 'RETURN_TO_SUPPLIER'
            )),
    CONSTRAINT chk_stock_movements_reference_type
        CHECK (reference_type IN (
                                  'PURCHASE_INVOICE',
                                  'ORDER',
                                  'TRANSFER',
                                  'RETURN'
            )),
    CONSTRAINT fk_stock_movements_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movements_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_movements_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_movement_product_date ON stock_movements (product_id, created_at);
CREATE INDEX idx_movement_warehouse    ON stock_movements (warehouse_id);
CREATE INDEX idx_movement_reference    ON stock_movements (reference_type, reference_id);
CREATE INDEX idx_movement_type         ON stock_movements (movement_type);



CREATE TABLE purchase_invoices
(
    id                   BIGSERIAL      NOT NULL,
    invoice_number       VARCHAR(50)    NOT NULL,
    company_id           BIGINT         NOT NULL,
    warehouse_id         BIGINT         NOT NULL,
    created_by           BIGINT         NOT NULL,
    approved_by          BIGINT         NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    invoice_date         DATE           NOT NULL,
    approved_at          TIMESTAMP      NULL,
    total_amount         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_deposit_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    notes                TEXT           NULL,
    created_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_purchase_invoices       PRIMARY KEY (id),
    CONSTRAINT uq_purchase_invoice_number UNIQUE (invoice_number),
    CONSTRAINT chk_purchase_invoices_status
        CHECK (status IN ('DRAFT', 'APPROVED', 'CANCELLED')),
    CONSTRAINT fk_purchase_invoices_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_purchase_invoices_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_purchase_invoices_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_purchase_invoices_approved_by
        FOREIGN KEY (approved_by) REFERENCES users (id)
);

CREATE INDEX idx_purchase_invoice_company   ON purchase_invoices (company_id);
CREATE INDEX idx_purchase_invoice_warehouse ON purchase_invoices (warehouse_id);
CREATE INDEX idx_purchase_invoice_date      ON purchase_invoices (invoice_date);
CREATE INDEX idx_purchase_invoice_status    ON purchase_invoices (status);



CREATE TABLE purchase_invoice_items
(
    id                  BIGSERIAL      NOT NULL,
    invoice_id          BIGINT         NOT NULL,
    product_id          BIGINT         NOT NULL,
    quantity            INTEGER        NOT NULL,
    purchase_price      NUMERIC(10, 2) NOT NULL,
    sale_price          NUMERIC(10, 2) NOT NULL,
    deposit_unit_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    line_total          NUMERIC(10, 2) NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_purchase_invoice_items PRIMARY KEY (id),
    CONSTRAINT chk_purchase_invoice_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT fk_purchase_invoice_items_invoice
        FOREIGN KEY (invoice_id) REFERENCES purchase_invoices (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_purchase_invoice_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_purchase_invoice_items_invoice ON purchase_invoice_items (invoice_id);
CREATE INDEX idx_purchase_invoice_items_product ON purchase_invoice_items (product_id);



CREATE TABLE stock_batches
(
    id                 BIGSERIAL      NOT NULL,
    invoice_item_id    BIGINT         NOT NULL,
    product_id         BIGINT         NOT NULL,
    warehouse_id       BIGINT         NOT NULL,
    initial_quantity   INTEGER        NOT NULL,
    remaining_quantity INTEGER        NOT NULL,
    purchase_price     NUMERIC(10, 2) NOT NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_stock_batches PRIMARY KEY (id),
    CONSTRAINT chk_stock_batches_initial_quantity
        CHECK (initial_quantity > 0),
    CONSTRAINT chk_stock_batches_remaining_quantity
        CHECK (remaining_quantity >= 0),
    CONSTRAINT chk_stock_batches_remaining_lte_initial
        CHECK (remaining_quantity <= initial_quantity),
    CONSTRAINT fk_stock_batches_invoice_item
        FOREIGN KEY (invoice_item_id) REFERENCES purchase_invoice_items (id),
    CONSTRAINT fk_stock_batches_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_batches_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE INDEX idx_batch_product_warehouse ON stock_batches (product_id, warehouse_id);
CREATE INDEX idx_batch_remaining
    ON stock_batches (product_id, warehouse_id, remaining_quantity)
    WHERE remaining_quantity > 0;
CREATE INDEX idx_batch_created ON stock_batches (created_at);


CREATE TABLE warehouse_transfers
(
    id                BIGSERIAL   NOT NULL,
    transfer_number   VARCHAR(50) NOT NULL,
    from_warehouse_id BIGINT      NOT NULL,
    to_warehouse_id   BIGINT      NOT NULL,
    created_by        BIGINT      NOT NULL,
    completed_by      BIGINT      NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at      TIMESTAMP   NULL,
    notes             TEXT        NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_warehouse_transfers     PRIMARY KEY (id),
    CONSTRAINT uq_transfer_number         UNIQUE (transfer_number),
    CONSTRAINT chk_transfer_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_transfer_different_warehouses
        CHECK (from_warehouse_id <> to_warehouse_id),
    CONSTRAINT fk_transfer_from_warehouse
        FOREIGN KEY (from_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_transfer_to_warehouse
        FOREIGN KEY (to_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_transfer_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_transfer_completed_by
        FOREIGN KEY (completed_by) REFERENCES users (id)
);

CREATE INDEX idx_transfer_from_warehouse ON warehouse_transfers (from_warehouse_id);
CREATE INDEX idx_transfer_to_warehouse   ON warehouse_transfers (to_warehouse_id);
CREATE INDEX idx_transfer_status         ON warehouse_transfers (status);



CREATE TABLE warehouse_transfer_items
(
    id          BIGSERIAL   NOT NULL,
    transfer_id BIGINT      NOT NULL,
    product_id  BIGINT      NOT NULL,
    quantity    INTEGER     NOT NULL,
    notes       TEXT        NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_warehouse_transfer_items PRIMARY KEY (id),
    CONSTRAINT chk_transfer_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT fk_transfer_items_transfer
        FOREIGN KEY (transfer_id) REFERENCES warehouse_transfers (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_transfer_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_transfer_items_transfer ON warehouse_transfer_items (transfer_id);
CREATE INDEX idx_transfer_items_product  ON warehouse_transfer_items (product_id);