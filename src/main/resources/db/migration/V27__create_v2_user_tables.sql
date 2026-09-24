CREATE TABLE tb_v2_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NULL,
    nickname VARCHAR(50) NULL,
    nickname_normalized VARCHAR(50) NULL,
    avatar_url VARCHAR(1000) NULL,
    bio VARCHAR(500) NULL,
    favorite_ramen_type VARCHAR(50) NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    log_count INT NOT NULL DEFAULT 0,
    onboarding_completed_at DATETIME(6) NULL,
    withdrawal_requested_at DATETIME(6) NULL,
    purge_scheduled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_user_nickname_normalized (nickname_normalized),
    KEY idx_v2_user_status_purge (status, purge_scheduled_at),
    CONSTRAINT chk_v2_user_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_v2_user_status CHECK (status IN ('ONBOARDING', 'ACTIVE', 'SUSPENDED', 'WITHDRAW_PENDING', 'WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_user_oauth_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255) NULL,
    apple_refresh_token_encrypted VARCHAR(2048) NULL,
    linked_at DATETIME(6) NOT NULL,
    last_login_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_user_oauth_provider_subject (provider, provider_subject),
    UNIQUE KEY uk_v2_user_oauth_user_provider (user_id, provider),
    CONSTRAINT chk_v2_user_oauth_provider CHECK (provider IN ('KAKAO', 'GOOGLE', 'APPLE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_user_consent (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    consent_type VARCHAR(20) NOT NULL,
    document_version VARCHAR(32) NOT NULL,
    granted BOOLEAN NOT NULL,
    decided_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_v2_user_consent_user_type (user_id, consent_type, decided_at),
    CONSTRAINT chk_v2_user_consent_type CHECK (consent_type IN ('TERMS', 'PRIVACY', 'MARKETING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
