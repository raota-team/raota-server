CREATE INDEX idx_bookmark_member_shop_deleted
ON tb_bookmark (member_profile_id, ramen_shop_id, is_deleted);

CREATE INDEX idx_proof_picture_member_shop_deleted
ON tb_ramen_proof_picture (member_id, ramen_shop_id, is_deleted);

CREATE INDEX idx_menu_vote_member_shop_cancelled
ON tb_menu_vote (member_id, ramen_shop_id, is_cancelled);
