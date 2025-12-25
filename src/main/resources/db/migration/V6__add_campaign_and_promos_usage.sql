CREATE TABLE promo_code_usages
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    promo_code_id    BIGINT         NOT NULL,
    order_id         BIGINT         NOT NULL,
    campaign_id      BIGINT,
    discount_applied DECIMAL(10, 2) NOT NULL,
    used_at          TIMESTAMP      NOT NULL,
    CONSTRAINT fk_promo_code_usage_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_promo_code_usage_promo FOREIGN KEY (promo_code_id) REFERENCES promos (id),
    CONSTRAINT fk_promo_code_usage_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE campaign_usages
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT    NOT NULL,
    campaign_id     BIGINT    NOT NULL,
    order_id        BIGINT    NOT NULL,
    buy_product_id  BIGINT    NOT NULL,
    buy_quantity    INTEGER   NOT NULL,
    free_product_id BIGINT    NOT NULL,
    free_quantity   INTEGER   NOT NULL,
    used_at         TIMESTAMP NOT NULL,
    CONSTRAINT fk_campaign_usage_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_campaign_usage_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id),
    CONSTRAINT fk_campaign_usage_order FOREIGN KEY (order_id) REFERENCES orders (id)
);