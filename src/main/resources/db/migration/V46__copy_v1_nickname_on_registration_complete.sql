-- V45의 웹(v1) → 앱(v2) 회원 복사 트리거에서 닉네임 규칙을 바꾼다.
--   * 닉네임은 웹 가입을 마쳤을 때만 복사한다. 가입을 마치기 전 닉네임(소셜 계정 이름)은 복사하지 않는다.
--   * 띄어쓰기는 밑줄(_)로 바꾸고 그 밖의 문자는 그대로 옮긴다.
--   * 비었거나 50자를 넘거나 앱에서 이미 쓰는 닉네임이면 비워 두고 온보딩으로 남긴다.
-- 나머지(시각, 상태, 탈퇴 회원 제외, IGNORE 쓰기)는 V45와 같다.

DROP TRIGGER IF EXISTS trg_member_profile_after_insert_copy_v2_user;
DROP TRIGGER IF EXISTS trg_member_profile_after_update_complete_v2_user;

DELIMITER $$

CREATE TRIGGER trg_member_profile_after_insert_copy_v2_user
AFTER INSERT ON tb_member_profile
FOR EACH ROW
BEGIN
    DECLARE copied_nickname VARCHAR(50) DEFAULT NULL;
    IF NEW.deleted_at IS NULL THEN
        IF NEW.is_registration_completed THEN
            SET copied_nickname = CASE
                WHEN CHAR_LENGTH(REPLACE(TRIM(NEW.nickname), ' ', '_')) BETWEEN 1 AND 50
                THEN REPLACE(TRIM(NEW.nickname), ' ', '_')
            END;
            IF EXISTS (SELECT 1 FROM tb_v2_user WHERE nickname_normalized = LOWER(copied_nickname)) THEN
                SET copied_nickname = NULL;
            END IF;
        END IF;
        INSERT IGNORE INTO tb_v2_user (id, email, nickname, nickname_normalized, avatar_url, bio, role, status,
                                       log_count, onboarding_completed_at, created_at, updated_at)
        VALUES (
            NEW.id,
            CASE WHEN CHAR_LENGTH(TRIM(NEW.email)) BETWEEN 1 AND 255 THEN TRIM(NEW.email) END,
            copied_nickname,
            LOWER(copied_nickname),
            NULLIF(TRIM(NEW.image_url), ''),
            NULLIF(NEW.bio, ''),
            'USER',
            CASE WHEN copied_nickname IS NOT NULL THEN 'ACTIVE' ELSE 'ONBOARDING' END,
            0,
            CASE WHEN copied_nickname IS NOT NULL THEN NEW.created_at - INTERVAL 9 HOUR END,
            NEW.created_at - INTERVAL 9 HOUR,
            NEW.updated_at - INTERVAL 9 HOUR);
    END IF;
END$$

CREATE TRIGGER trg_member_profile_after_update_complete_v2_user
AFTER UPDATE ON tb_member_profile
FOR EACH ROW
BEGIN
    DECLARE copied_nickname VARCHAR(50) DEFAULT NULL;
    IF NOT OLD.is_registration_completed AND NEW.is_registration_completed AND NEW.deleted_at IS NULL THEN
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

-- 웹 가입을 마쳤지만 닉네임 규칙 때문에 온보딩으로 남은 앱 회원에게 바뀐 규칙으로 닉네임을 넣는다(운영 36, 48, 59, 72, 76, 93).
UPDATE IGNORE tb_v2_user u
JOIN tb_member_profile m ON m.id = u.id AND u.created_at = m.created_at - INTERVAL 9 HOUR
LEFT JOIN tb_v2_user taken
    ON taken.nickname_normalized = LOWER(REPLACE(TRIM(m.nickname), ' ', '_')) AND taken.id <> u.id
SET u.nickname = REPLACE(TRIM(m.nickname), ' ', '_'),
    u.nickname_normalized = LOWER(REPLACE(TRIM(m.nickname), ' ', '_')),
    u.status = 'ACTIVE',
    u.onboarding_completed_at = m.updated_at - INTERVAL 9 HOUR,
    u.updated_at = m.updated_at - INTERVAL 9 HOUR
WHERE u.status = 'ONBOARDING'
    AND m.is_registration_completed
    AND m.deleted_at IS NULL
    AND CHAR_LENGTH(REPLACE(TRIM(m.nickname), ' ', '_')) BETWEEN 1 AND 50
    AND taken.id IS NULL;
