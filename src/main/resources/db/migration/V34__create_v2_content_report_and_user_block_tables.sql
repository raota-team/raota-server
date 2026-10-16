CREATE TABLE tb_v2_content_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_content_report_reporter_target (reporter_id, target_type, target_id),
    KEY idx_v2_content_report_target (target_type, target_id),
    CONSTRAINT chk_v2_content_report_target_type CHECK (target_type IN ('RAMEN_LOG', 'RAMEN_LOG_COMMENT')),
    CONSTRAINT chk_v2_content_report_reason CHECK (reason IN ('SPAM', 'ABUSE', 'SEXUAL', 'PRIVACY', 'OTHER')),
    CONSTRAINT chk_v2_content_report_status CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_user_block (
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id),
    KEY idx_v2_user_block_blocked (blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
