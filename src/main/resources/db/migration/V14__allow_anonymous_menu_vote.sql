ALTER TABLE tb_menu_vote MODIFY member_id BIGINT NULL;
ALTER TABLE tb_menu_vote ADD COLUMN anonymous_voter_id VARCHAR(36) NULL AFTER member_id;
