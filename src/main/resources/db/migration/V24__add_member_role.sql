ALTER TABLE tb_member_profile
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD CONSTRAINT chk_member_profile_role CHECK (role IN ('USER', 'ADMIN'));
