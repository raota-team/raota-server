-- 1. 라멘 종류 테이블 생성
CREATE TABLE tb_ramen_type(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sub_title TEXT NOT NULL,
    image_url TEXT NOT NULL
);

-- 2. 주간 큐레이션 테이블 생성
CREATE TABLE tb_weekend_curation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year_week INT NOT NULL, -- 형식: YYYYWW (예: 202425)
    ramen_type_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    custom_image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_weekend_curation_year_week UNIQUE (year_week),
    CONSTRAINT fk_weekend_curation_type FOREIGN KEY (ramen_type_id) REFERENCES tb_ramen_type(id)
);
