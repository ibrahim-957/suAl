CREATE TABLE IF NOT EXISTS container_collections
(
    id                   BIGSERIAL PRIMARY KEY,
    warehouse_id         BIGINT    NOT NULL,
    product_id           BIGINT    NOT NULL,
    empty_containers     INTEGER   NOT NULL,
    damaged_containers   INTEGER   NOT NULL,
    total_collected      INTEGER   NOT NULL,
    collected_by_user_id BIGINT,
    collection_date_time TIMESTAMP NOT NULL,
    notes                VARCHAR(500),
    created_at           TIMESTAMP NOT NULL,

    CONSTRAINT fk_container_collection_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE,

    CONSTRAINT fk_container_collection_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,

    CONSTRAINT fk_container_collection_user
        FOREIGN KEY (collected_by_user_id) REFERENCES users (id) ON DELETE SET NULL,

    CONSTRAINT chk_containers_positive
        CHECK (empty_containers >= 0 AND damaged_containers >= 0),

    CONSTRAINT chk_total_matches
        CHECK (total_collected = empty_containers + damaged_containers)
);