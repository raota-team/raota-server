-- 웹(v1) 가입을 마칠 때 닉네임과 함께 이메일·소개·프로필 사진도 같은 사람의 앱(v2) 회원에 반영한다.
--   * 웹은 소셜 로그인으로 회원을 먼저 만들고 가입을 마칠 때 이 값들을 채운다. V46까지는 닉네임과 상태만 반영했다.
--   * 이메일·소개·프로필 사진은 앱 회원에서 비어 있을 때만 채운다. 앱에서 이미 정한 값은 덮어쓰지 않는다.
-- 닉네임 규칙은 V46과 같다.

DROP TRIGGER IF EXISTS trg_member_profile_after_update_complete_v2_user;

DELIMITER $$

CREATE TRIGGER trg_member_profile_after_update_complete_v2_user
AFTER UPDATE ON tb_member_profile
FOR EACH ROW
BEGIN
    DECLARE copied_nickname VARCHAR(50) DEFAULT NULL;
    IF NOT OLD.is_registration_completed AND NEW.is_registration_completed AND NEW.deleted_at IS NULL THEN
        UPDATE IGNORE tb_v2_user
        SET email = COALESCE(email, CASE WHEN CHAR_LENGTH(TRIM(NEW.email)) BETWEEN 1 AND 255 THEN TRIM(NEW.email) END),
            bio = COALESCE(bio, CASE WHEN CHAR_LENGTH(NEW.bio) BETWEEN 1 AND 500 THEN NEW.bio END),
            avatar_url = COALESCE(avatar_url,
                CASE WHEN CHAR_LENGTH(TRIM(NEW.image_url)) BETWEEN 1 AND 1000 THEN TRIM(NEW.image_url) END)
        WHERE id = NEW.id
            AND created_at = NEW.created_at - INTERVAL 9 HOUR;

        SET copied_nickname = CASE
            WHEN CHAR_LENGTH(REPLACE(TRIM(NEW.nickname), ' ', '_')) BETWEEN 1 AND 50
            THEN REPLACE(TRIM(NEW.nickname), ' ', '_')
        END;
        IF copied_nickname IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM tb_v2_user WHERE nickname_normalized = LOWER(copied_nickname) AND id <> NEW.id) THEN
            UPDATE IGNORE tb_v2_user
            SET nickname = copied_nickname,
                nickname_normalized = LOWER(copied_nickname),
                status = 'ACTIVE',
                onboarding_completed_at = NEW.updated_at - INTERVAL 9 HOUR,
                updated_at = NEW.updated_at - INTERVAL 9 HOUR
            WHERE id = NEW.id
                AND created_at = NEW.created_at - INTERVAL 9 HOUR
                AND status = 'ONBOARDING';
        END IF;
    END IF;
END$$

DELIMITER ;
