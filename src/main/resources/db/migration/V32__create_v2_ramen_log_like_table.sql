CREATE TABLE tb_v2_ramen_log_like (
    user_id BIGINT NOT NULL,
    ramen_log_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, ramen_log_id),
    KEY idx_v2_ramen_log_like_log (ramen_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE tb_v2_ramen_log
    ADD KEY idx_v2_ramen_log_feed_likes (visibility, deleted_at, like_count, id);
