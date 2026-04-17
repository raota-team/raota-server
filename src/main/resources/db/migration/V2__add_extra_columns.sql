ALTER TABLE tb_ramen_shop
ADD COLUMN branch_name VARCHAR(255) AFTER name,
ADD COLUMN naver_map_id VARCHAR(100) AFTER ramen_shop_id,
ADD COLUMN latitude DECIMAL(10, 8) AFTER detail,
ADD COLUMN longitude DECIMAL(11, 8) AFTER latitude;
