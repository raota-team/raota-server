CREATE TABLE tb_v2_shop (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(1000) NOT NULL,
    branch_name VARCHAR(255) NULL,
    address TEXT NOT NULL,
    region VARCHAR(1000) NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    phone VARCHAR(100) NULL,
    tagline TEXT NULL,
    description TEXT NULL,
    ramen_types JSON NOT NULL,
    tags JSON NOT NULL,
    instagram_url VARCHAR(1000) NULL,
    reservation_url VARCHAR(1000) NULL,
    website_url VARCHAR(1000) NULL,
    naver_place_id VARCHAR(100) NULL,
    kakao_place_id VARCHAR(100) NULL,
    price_min INT NULL,
    price_max INT NULL,
    business_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    closed_days_text VARCHAR(50) NULL,
    hours_verified_at DATETIME(6) NULL,
    ai_review_summary TEXT NULL,
    ai_summary_keywords JSON NOT NULL,
    ai_summary_generated_at DATETIME(6) NULL,
    view_count INT NOT NULL DEFAULT 0,
    log_count INT NOT NULL DEFAULT 0,
    bookmark_count INT NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_v2_shop_published_deleted (is_published, deleted_at),
    KEY idx_v2_shop_popular (view_count, id),
    KEY idx_v2_shop_latest (created_at, id),
    CONSTRAINT chk_v2_shop_business_status CHECK (business_status IN ('OPERATIONAL', 'TEMPORARILY_CLOSED', 'CLOSED', 'UNKNOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_shop_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    url VARCHAR(1000) NOT NULL,
    source VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_v2_shop_image_shop (shop_id),
    CONSTRAINT chk_v2_shop_image_source CHECK (source IN ('OWNER', 'USER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_shop_business_hour (
    id BIGINT NOT NULL AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL,
    opens_at TIME NULL,
    closes_at TIME NULL,
    break_start TIME NULL,
    break_end TIME NULL,
    last_order_at TIME NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_shop_hour_day (shop_id, day_of_week),
    KEY idx_v2_shop_hour_shop (shop_id),
    CONSTRAINT chk_v2_shop_hour_day CHECK (day_of_week BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_shop_service_perk (
    id BIGINT NOT NULL AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    perk_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    price INT NULL,
    condition_text VARCHAR(1000) NULL,
    verified_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_v2_shop_perk_type (shop_id, perk_type),
    KEY idx_v2_shop_perk_shop (shop_id),
    CONSTRAINT chk_v2_shop_perk_type CHECK (perk_type IN ('NOODLE_REFILL', 'RICE_REFILL', 'SOUP_REFILL', 'CONDIMENT')),
    CONSTRAINT chk_v2_shop_perk_status CHECK (status IN ('FREE', 'PAID', 'NOT_OFFERED', 'UNKNOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_v2_shop_bookmark (
    user_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, shop_id),
    KEY idx_v2_shop_bookmark_user_created (user_id, created_at),
    KEY idx_v2_shop_bookmark_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
