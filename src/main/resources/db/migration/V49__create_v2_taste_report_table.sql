-- 취향 리포트: 뽑을 때의 집계 숫자를 굳혀 둔다. 기준일(cutoff_date)까지 visited_at인 기록이 들어간다.
CREATE TABLE tb_v2_taste_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    cutoff_date DATE NOT NULL,
    kind VARCHAR(10) NOT NULL,
    bowl_count INT NOT NULL,
    shop_count INT NOT NULL,
    type_counts JSON NOT NULL,
    axis_averages JSON NOT NULL,
    top_shops JSON NOT NULL,
    recent_log_ids JSON NOT NULL,
    seen_at DATETIME(6) NULL,
    notified_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_taste_report_user_cutoff (user_id, cutoff_date),
    KEY idx_v2_taste_report_cutoff (cutoff_date),
    CONSTRAINT chk_v2_taste_report_kind CHECK (kind IN ('FIRST', 'MONTHLY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
