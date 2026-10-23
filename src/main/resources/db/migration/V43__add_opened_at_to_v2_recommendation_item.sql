-- 추천 결과에서 회원이 매장을 처음 연 시각이다. 관리자 "AI 추천 성과"의 열기율을 계산한다.
-- 찜·기록 전환은 기존 찜·기록 테이블의 생성 시각으로 계산하므로 따로 남기지 않는다.
ALTER TABLE tb_v2_recommendation_item ADD COLUMN opened_at DATETIME(6) NULL;
