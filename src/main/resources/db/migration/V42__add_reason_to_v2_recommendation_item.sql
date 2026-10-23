-- 앱 "오늘 뽑은 식권 다시 보기"가 추천 때 보여준 이유를 그대로 다시 보여 주도록 매장별 추천 이유를 남긴다.
-- 이유는 서버에서 260자로 자르므로(MAX_REASON_LENGTH) 넉넉히 둔다.
ALTER TABLE tb_v2_recommendation_item ADD COLUMN reason VARCHAR(500) NULL;
