CREATE TABLE tb_v2_ramen_log_comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ramen_log_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_ramen_log_comment_user_key (user_id, idempotency_key),
    KEY idx_v2_ramen_log_comment_log (ramen_log_id, deleted_at, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
