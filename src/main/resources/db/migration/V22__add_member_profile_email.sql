ALTER TABLE tb_member_profile
    ADD COLUMN email VARCHAR(1000) NULL AFTER nickname,
    ADD INDEX idx_member_profile_email (email(191));

UPDATE tb_member_profile m
JOIN (
    SELECT member_id, MAX(email) AS email
    FROM tb_social_account
    WHERE email IS NOT NULL AND email <> ''
    GROUP BY member_id
) sa ON sa.member_id = m.id
SET m.email = sa.email
WHERE m.email IS NULL;
