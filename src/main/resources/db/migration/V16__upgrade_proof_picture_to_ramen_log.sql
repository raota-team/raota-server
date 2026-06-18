RENAME TABLE tb_ramen_proof_picture TO tb_ramen_log;

ALTER TABLE tb_ramen_log
    CHANGE COLUMN description note TEXT,
    CHANGE COLUMN uploaded_at created_at DATETIME(6) NOT NULL,
    ADD COLUMN ramen_type VARCHAR(50) NOT NULL DEFAULT '기타' AFTER menu_name,
    ADD COLUMN broth_notes JSON AFTER note,
    ADD COLUMN noodle_notes JSON AFTER broth_notes,
    ADD COLUMN seasoning_notes JSON AFTER noodle_notes,
    ADD COLUMN topping_notes JSON AFTER seasoning_notes,
    ADD COLUMN revisit VARCHAR(30) NOT NULL DEFAULT 'SOMETIMES' AFTER topping_notes,
    ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT TRUE AFTER revisit,
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0 AFTER is_public,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at,
    ADD INDEX idx_ramen_log_public_created (is_public, is_deleted, created_at),
    ADD INDEX idx_ramen_log_member_created (member_id, is_deleted, created_at),
    ADD INDEX idx_ramen_log_shop_created (ramen_shop_id, is_deleted, created_at),
    ADD INDEX idx_ramen_log_popular (is_public, is_deleted, like_count, created_at);

CREATE TABLE tb_ramen_log_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ramen_log_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_ramen_log_like UNIQUE (ramen_log_id, member_id),
    INDEX idx_ramen_log_like_member (member_id)
);
