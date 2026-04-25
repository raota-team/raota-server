-- 게시글 및 댓글 소프트 딜리트 컬럼 추가
ALTER TABLE tb_post ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tb_comment ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 게시글 좋아요 테이블 생성
CREATE TABLE tb_post_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_post_member (post_id, member_id)
);
