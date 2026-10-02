-- v1에 공개된 매장은 영업 매장으로 취급했고 숨김 매장은 운영 여부를 확인할 수 없었다.
-- 기존 v2 행은 앱의 원본이므로 업데이트하지 않는다.
INSERT INTO tb_v2_shop (
    id, name, branch_name, address, region, latitude, longitude, phone, tagline, description,
    ramen_types, tags, instagram_url, reservation_url, website_url, naver_place_id, kakao_place_id,
    price_min, price_max, business_status, closed_days_text, hours_verified_at,
    ai_review_summary, ai_summary_keywords, ai_summary_generated_at, view_count, log_count,
    bookmark_count, is_published, created_at, updated_at, deleted_at
)
WITH tag_types AS (
    SELECT DISTINCT source.ramen_shop_id,
        CASE
            WHEN tag.value LIKE '%쇼유%' OR tag.value LIKE '%쇼우%' THEN '쇼유'
            WHEN tag.value LIKE '%돈코츠%' OR tag.value LIKE '%톤코츠%' THEN '돈코츠'
            WHEN tag.value LIKE '%시오%' THEN '시오'
            WHEN tag.value LIKE '%미소%' THEN '미소'
            WHEN tag.value LIKE '%츠케멘%' THEN '츠케멘'
            WHEN tag.value LIKE '%탄탄멘%' OR tag.value LIKE '%담담멘%' THEN '탄탄멘'
            WHEN tag.value LIKE '%마제소바%' THEN '마제소바'
            WHEN tag.value LIKE '%아부라소바%' THEN '아부라소바'
        END AS ramen_type
    FROM tb_ramen_shop source
    JOIN JSON_TABLE(COALESCE(source.tags, JSON_ARRAY()), '$[*]'
        COLUMNS (value VARCHAR(255) PATH '$')) tag
), types_by_shop AS (
    SELECT ramen_shop_id, JSON_ARRAYAGG(ramen_type) AS ramen_types
    FROM tag_types
    WHERE ramen_type IS NOT NULL
    GROUP BY ramen_shop_id
)
SELECT source.ramen_shop_id, source.name, source.branch_name,
    CONCAT_WS(' ', NULLIF(TRIM(source.city), ''), NULLIF(TRIM(source.district), ''),
        NULLIF(TRIM(source.street), ''), NULLIF(TRIM(source.detail), '')),
    COALESCE(NULLIF(TRIM(source.district), ''), NULLIF(TRIM(source.city), '')),
    source.latitude, source.longitude, NULL, source.description, source.detailed_description,
    COALESCE(types_by_shop.ramen_types, JSON_ARRAY()), COALESCE(source.tags, JSON_ARRAY()),
    source.instagram_url, source.catch_table_url, NULL, source.naver_map_id, NULL,
    NULL, NULL, CASE WHEN source.is_published THEN 'OPERATIONAL' ELSE 'UNKNOWN' END,
    source.closed_days, NULL, NULL, JSON_ARRAY(), NULL, source.view_count, 0, 0,
    source.is_published, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tb_ramen_shop source
LEFT JOIN types_by_shop ON types_by_shop.ramen_shop_id = source.ramen_shop_id
WHERE NOT EXISTS (SELECT 1 FROM tb_v2_shop target WHERE target.id = source.ramen_shop_id);

-- 자식 행은 매장에 같은 종류의 자식 행이 전혀 없을 때만 복사한다.
-- 따라서 재실행 시 중복되지 않고 v2에서 작성한 자식 데이터도 덮어쓰지 않는다.
INSERT INTO tb_v2_shop_image (shop_id, url, source, sort_order)
SELECT source.ramen_shop_id, source.image_url, 'ADMIN', 0
FROM tb_ramen_shop source
JOIN tb_v2_shop target ON target.id = source.ramen_shop_id
WHERE NULLIF(TRIM(source.image_url), '') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tb_v2_shop_image image WHERE image.shop_id = source.ramen_shop_id);

INSERT INTO tb_v2_shop_business_hour (
    shop_id, day_of_week, opens_at, closes_at, break_start, break_end, last_order_at, is_closed
)
SELECT source.ramen_shop_id, days.day_of_week, source.open_time, source.close_time,
    source.break_start, source.break_end, NULL, FALSE
FROM tb_ramen_shop source
JOIN tb_v2_shop target ON target.id = source.ramen_shop_id
CROSS JOIN (
    SELECT 1 AS day_of_week UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) days
WHERE (source.open_time IS NOT NULL OR source.close_time IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM tb_v2_shop_business_hour hour
                  WHERE hour.shop_id = source.ramen_shop_id);
