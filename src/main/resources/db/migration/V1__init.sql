-- V1: 초기 스키마 구축 (물리적 외래키 배제, tb_ 접두사 적용)

CREATE TABLE tb_member_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(1000) NOT NULL,
    image_url VARCHAR(1000),
    background_image_url VARCHAR(1000),
    post_count BIGINT DEFAULT 0,
    comment_count BIGINT DEFAULT 0
);

CREATE TABLE tb_social_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    provider VARCHAR(255) NOT NULL,
    provider_id VARCHAR(1000) NOT NULL,
    email VARCHAR(1000)
);

CREATE TABLE tb_refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    token VARCHAR(1000) NOT NULL,
    expiry_date DATETIME NOT NULL
);

CREATE TABLE tb_ramen_shop (
    ramen_shop_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(1000) NOT NULL,
    -- Address (Embedded)
    city VARCHAR(1000),
    district VARCHAR(1000),
    detail_address VARCHAR(1000),
    zip_code VARCHAR(1000),
    -- BusinessHours (Embedded)
    open_time TIME,
    close_time TIME,
    last_order_time TIME,
    -- Stats (Embedded)
    total_rating DECIMAL(10, 2) DEFAULT 0,
    review_count BIGINT DEFAULT 0,
    visit_count BIGINT DEFAULT 0,
    bookmark_count BIGINT DEFAULT 0,
    -- Metadata
    tags JSON,
    instagram_url VARCHAR(1000),
    catch_table_url VARCHAR(1000),
    description TEXT,
    image_url VARCHAR(1000)
);

CREATE TABLE tb_normal_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ramen_shop_id BIGINT NOT NULL,
    name VARCHAR(1000) NOT NULL,
    price INT NOT NULL,
    image_url VARCHAR(1000)
);

CREATE TABLE tb_event_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ramen_shop_id BIGINT NOT NULL,
    name VARCHAR(1000) NOT NULL,
    price INT NOT NULL,
    image_url VARCHAR(1000),
    start_date DATE,
    end_date DATE
);

CREATE TABLE tb_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255) NOT NULL,
    title VARCHAR(1000) NOT NULL,
    content LONGTEXT NOT NULL,
    content_format VARCHAR(255),
    thumbnail_url VARCHAR(1000),
    author_id BIGINT NOT NULL,
    ramen_shop_id BIGINT,
    created_at DATETIME NOT NULL
);

CREATE TABLE tb_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    created_at DATETIME NOT NULL
);

CREATE TABLE tb_bookmark (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_profile_id BIGINT NOT NULL,
    ramen_shop_id BIGINT NOT NULL,
    marking_at DATETIME NOT NULL
);

CREATE TABLE tb_menu_vote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    ramen_shop_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    voted_at DATETIME NOT NULL
);

CREATE TABLE tb_ramen_proof_picture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ramen_shop_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    description TEXT,
    uploaded_at DATETIME NOT NULL
);

CREATE TABLE tb_ramen_shop_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ramen_shop_id BIGINT NOT NULL,
    member_profile_id BIGINT NOT NULL,
    report_type VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    reported_at DATETIME NOT NULL
);
