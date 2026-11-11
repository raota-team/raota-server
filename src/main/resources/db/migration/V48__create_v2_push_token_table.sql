-- Expo 푸시 토큰. 한 기기의 토큰은 마지막으로 등록한 회원 하나에만 속한다.
CREATE TABLE tb_v2_push_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    platform VARCHAR(10) NOT NULL,
    language VARCHAR(5) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_push_token_token (token),
    KEY idx_v2_push_token_user (user_id),
    CONSTRAINT chk_v2_push_token_platform CHECK (platform IN ('IOS', 'ANDROID')),
    CONSTRAINT chk_v2_push_token_language CHECK (language IN ('KO', 'EN', 'JA', 'ZH', 'ES'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
