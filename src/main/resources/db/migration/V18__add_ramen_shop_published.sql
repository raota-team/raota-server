ALTER TABLE tb_ramen_shop
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_ramen_shop_published
    ON tb_ramen_shop (is_published, ramen_shop_id);
