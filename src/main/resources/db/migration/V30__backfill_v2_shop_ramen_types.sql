-- V29가 찾지 못한 라멘 종류를 v2 tags에서 다시 찾는다. v1 테이블은 읽지 않는다.
-- 여러 번 실행해도 결과가 같다.

-- 이에케는 돈코츠와 다른 독립 종류다. 태그에 이에케가 있으면 기존 종류는 두고 뒤에 덧붙인다.
UPDATE tb_v2_shop
SET ramen_types = JSON_ARRAY_APPEND(ramen_types, '$', '이에케'),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE JSON_SEARCH(tags, 'one', '%이에케%') IS NOT NULL
  AND NOT JSON_CONTAINS(ramen_types, JSON_QUOTE('이에케'));

-- 아래는 종류가 아직 비어 있는 매장만 채운다. 지역·상황 태그만 있어 종류를 알 수 없는 매장은 비워 두고 운영자가 채운다.
UPDATE tb_v2_shop
SET ramen_types = JSON_ARRAY('탄탄멘'),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE JSON_LENGTH(ramen_types) = 0
  AND JSON_SEARCH(tags, 'one', '%탄탄면%') IS NOT NULL;

-- 앱의 종류 목록에 없는 장르는 기타로 둔다.
UPDATE tb_v2_shop
SET ramen_types = JSON_ARRAY('기타'),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE JSON_LENGTH(ramen_types) = 0
  AND (JSON_SEARCH(tags, 'one', '%지로%') IS NOT NULL
    OR JSON_SEARCH(tags, 'one', '%파이탄%') IS NOT NULL
    OR JSON_SEARCH(tags, 'one', '%닭육수%') IS NOT NULL
    OR JSON_SEARCH(tags, 'one', '%니보시%') IS NOT NULL
    OR JSON_SEARCH(tags, 'one', '%츄카소바%') IS NOT NULL
    OR JSON_SEARCH(tags, 'one', '%중화소바%') IS NOT NULL
    OR JSON_SEARCH(tags, 'one', '%시루나시%') IS NOT NULL
    OR JSON_SEARCH(tags, 'one', '%참돔%') IS NOT NULL);
