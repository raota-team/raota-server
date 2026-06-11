ALTER TABLE tb_weekend_curation
    ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT '이번 주말의 라멘 추천' AFTER ramen_type_id;
