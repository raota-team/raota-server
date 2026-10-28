-- 지난 식권 목록(GET /api/v2/ai/recommendations)은 회원별로 created_at DESC, id DESC 커서로 읽는다.
-- 이 인덱스의 앞부분(user_id)이 기존 회원 인덱스 역할도 하므로 단일 user_id 인덱스는 지운다.
ALTER TABLE tb_v2_recommendation_run
    ADD INDEX idx_v2_recommendation_run_user_created (user_id, created_at, id),
    DROP INDEX idx_v2_recommendation_run_user;
