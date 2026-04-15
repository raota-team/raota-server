CREATE TABLE tb_member_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(1000) NOT NULL,
    image_url VARCHAR(1000),
    background_image_url VARCHAR(1000),
    visited_restaurant_count INT DEFAULT 0,
    photo_count INT DEFAULT 0,
    bookmark_count INT DEFAULT 0,
    post_count INT DEFAULT 0,
    comment_count INT DEFAULT 0
);

CREATE TABLE tb_social_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    email VARCHAR(1000),
    nickname VARCHAR(1000) NOT NULL,
    profile_image_url VARCHAR(1000)
);

CREATE TABLE tb_refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    token VARCHAR(200) NOT NULL UNIQUE,
    expiry_date DATETIME(6) NOT NULL
);

CREATE TABLE tb_ramen_shop (
    ramen_shop_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(1000) NOT NULL,
    city VARCHAR(1000),
    district VARCHAR(1000),
    street VARCHAR(1000),
    detail VARCHAR(1000),
    closed_days VARCHAR(50),
    open_time TIME,
    close_time TIME,
    break_start TIME,
    break_end TIME,
    parking_info VARCHAR(100),
    visit_count INT DEFAULT 0,
    bookmark_count INT DEFAULT 0,
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
    is_signature BOOLEAN NOT NULL DEFAULT FALSE,
    image_url VARCHAR(1000)
);

CREATE TABLE tb_event_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ramen_shop_id BIGINT NOT NULL,
    name VARCHAR(1000) NOT NULL,
    description TEXT,
    price INT NOT NULL,
    badge_text VARCHAR(1000),
    start_date DATE,
    end_date DATE,
    image_url VARCHAR(1000)
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
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE tb_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE tb_bookmark (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_profile_id BIGINT NOT NULL,
    ramen_shop_id BIGINT NOT NULL,
    marking_at DATETIME(6) NOT NULL
);

CREATE TABLE tb_menu_vote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    ramen_shop_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    voted_at DATETIME(6) NOT NULL
);

CREATE TABLE tb_ramen_proof_picture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ramen_shop_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    image_name VARCHAR(1000),
    image_url VARCHAR(1000) NOT NULL,
    description TEXT,
    uploaded_at DATETIME(6) NOT NULL
);

CREATE TABLE tb_ramen_shop_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ramen_shop_id BIGINT NOT NULL,
    member_profile_id BIGINT NOT NULL,
    report_type VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    reported_at DATETIME(6) NOT NULL
);
