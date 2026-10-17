CREATE TABLE tb_v2_recommendation_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    conditions JSON NOT NULL,
    latitude DECIMAL(5, 2) NULL,
    longitude DECIMAL(6, 2) NULL,
    candidate_count INT NOT NULL,
    algorithm_version VARCHAR(50) NOT NULL,
    index_version VARCHAR(100) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    latency_ms INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_v2_recommendation_run_user (user_id),
    KEY idx_v2_recommendation_run_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_recommendation_item (
    run_id BIGINT NOT NULL,
    item_rank INT NOT NULL,
    shop_id BIGINT NOT NULL,
    score DOUBLE NOT NULL,
    PRIMARY KEY (run_id, item_rank),
    KEY idx_v2_recommendation_item_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
