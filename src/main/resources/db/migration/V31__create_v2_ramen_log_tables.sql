CREATE TABLE tb_v2_ramen_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    visited_at DATE NOT NULL,
    menu_name VARCHAR(150) NOT NULL,
    ramen_type VARCHAR(50) NOT NULL,
    satisfaction_score TINYINT NULL,
    broth_density_score TINYINT NULL,
    noodle_firmness_score TINYINT NULL,
    topping_score TINYINT NULL,
    revisit_intention VARCHAR(20) NULL,
    note VARCHAR(500) NOT NULL DEFAULT '',
    taste_note_codes JSON NOT NULL,
    visibility VARCHAR(10) NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_ramen_log_user_key (user_id, idempotency_key),
    KEY idx_v2_ramen_log_feed (visibility, deleted_at, created_at, id),
    KEY idx_v2_ramen_log_user_visit (user_id, visited_at, id),
    KEY idx_v2_ramen_log_shop_created (shop_id, created_at, id),
    CONSTRAINT chk_v2_ramen_log_satisfaction CHECK (satisfaction_score BETWEEN 1 AND 5),
    CONSTRAINT chk_v2_ramen_log_broth CHECK (broth_density_score BETWEEN 1 AND 5),
    CONSTRAINT chk_v2_ramen_log_noodle CHECK (noodle_firmness_score BETWEEN 1 AND 5),
    CONSTRAINT chk_v2_ramen_log_topping CHECK (topping_score BETWEEN 1 AND 5),
    CONSTRAINT chk_v2_ramen_log_revisit CHECK (revisit_intention IN ('OFTEN', 'SOMETIMES', 'ONCE_IS_ENOUGH')),
    CONSTRAINT chk_v2_ramen_log_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_ramen_log_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ramen_log_id BIGINT NOT NULL,
    url VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_ramen_log_image_order (ramen_log_id, sort_order),
    CONSTRAINT chk_v2_ramen_log_image_order CHECK (sort_order BETWEEN 0 AND 2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE tb_v2_shop
    ADD satisfaction_score_sum INT NOT NULL DEFAULT 0,
    ADD scored_log_count INT NOT NULL DEFAULT 0;
