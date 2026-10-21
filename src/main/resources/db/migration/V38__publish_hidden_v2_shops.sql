-- v1에서 비공개였던 매장도 앱에서 모두 보이게 한다.
-- V29가 비공개 매장을 business_status 'UNKNOWN'으로 옮겼으니 그 조건으로 고른다. 관리자가 앱에서 직접 숨긴 매장은
-- OPERATIONAL 등 다른 상태라 다시 열리지 않는다.
-- 554(클준빛날영)·759(라멘광훈)는 이미 공개된 800·799와 같은 가게를 중복 등록한 것이라 숨긴 채로 둔다.
UPDATE tb_v2_shop
SET is_published = TRUE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE is_published = FALSE
  AND deleted_at IS NULL
  AND business_status = 'UNKNOWN'
  AND id NOT IN (554, 759);
