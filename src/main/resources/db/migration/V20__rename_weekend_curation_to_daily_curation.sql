ALTER TABLE tb_weekend_curation
    RENAME TO tb_daily_curation;

ALTER TABLE tb_daily_curation
    RENAME COLUMN year_week TO date_key;

ALTER TABLE tb_daily_curation
    RENAME INDEX uk_weekend_curation_year_week TO uk_daily_curation_date_key;

ALTER TABLE tb_daily_curation
    DROP FOREIGN KEY fk_weekend_curation_type;

ALTER TABLE tb_daily_curation
    ADD CONSTRAINT fk_daily_curation_type
        FOREIGN KEY (ramen_type_id) REFERENCES tb_ramen_type(id);
