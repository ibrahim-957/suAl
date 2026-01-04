CREATE TABLE baskets
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_basket_user_id UNIQUE (user_id),
    CONSTRAINT fk_baskets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE basket_items
(
    id         BIGSERIAL PRIMARY KEY,
    basket_id  BIGINT    NOT NULL,
    product_id BIGINT    NOT NULL,
    quantity   INTEGER   NOT NULL CHECK (quantity >= 1),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_basket_product UNIQUE (basket_id, product_id),
    CONSTRAINT fk_basket_items_basket FOREIGN KEY (basket_id) REFERENCES baskets(id) ON DELETE CASCADE,
    CONSTRAINT fk_basket_item_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);