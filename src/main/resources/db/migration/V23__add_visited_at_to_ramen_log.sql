ALTER TABLE tb_ramen_log
    ADD COLUMN visited_at DATE NULL AFTER like_count;

UPDATE tb_ramen_log
SET visited_at = DATE(created_at)
WHERE visited_at IS NULL;

ALTER TABLE tb_ramen_log
    MODIFY COLUMN visited_at DATE NOT NULL,
    ADD INDEX idx_ramen_log_visited_at (visited_at, is_deleted, is_public),
    ADD INDEX idx_ramen_log_member_visited_at (member_id, is_deleted, visited_at);
