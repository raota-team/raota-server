-- 웹(v1) 회원 가입을 앱(v2) 회원으로 같은 ID로 복사한다(#120). 규칙은 82cb8e3에서 지운 MobileMemberCopyService와 같다.
--   * 시각: v1은 KST(LocalDateTime, JVM Asia/Seoul)로, v2는 UTC로 저장하므로 9시간을 뺀다.
--   * 닉네임: 2~12자의 한글·영문·숫자·밑줄이고 v2에서 비어 있을 때만 쓴다. 아니면 비워 두고 앱 온보딩에서 정한다.
--   * 상태: 가입을 마쳤고 닉네임을 썼으면 ACTIVE, 아니면 ONBOARDING. 탈퇴한 v1 회원은 옮기지 않는다.
--   * 소셜 계정: 같은 사람(같은 ID, 같은 생성 시각)으로 복사된 v2 회원에게만 붙인다.
-- 트리거 안의 쓰기는 모두 IGNORE라서 v2 쪽에서 막혀도(ID·닉네임·소셜 계정 중복) 웹 가입은 실패하지 않는다.
-- 앱 가입 ID는 운영에서 10,000,000부터 시작해(2026-10-31 수동 변경) 이후 v1 ID와 겹치지 않는다.

DELIMITER $$

CREATE TRIGGER trg_member_profile_after_insert_copy_v2_user
AFTER INSERT ON tb_member_profile
FOR EACH ROW
BEGIN
    DECLARE copied_nickname VARCHAR(50) DEFAULT NULL;
    IF NEW.deleted_at IS NULL THEN
        IF TRIM(NEW.nickname) REGEXP '^[가-힣a-zA-Z0-9_]{2,12}$'
            AND NOT EXISTS (
                SELECT 1 FROM tb_v2_user WHERE nickname_normalized = LOWER(TRIM(NEW.nickname))) THEN
            SET copied_nickname = TRIM(NEW.nickname);
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
            CASE WHEN NEW.is_registration_completed AND copied_nickname IS NOT NULL THEN 'ACTIVE' ELSE 'ONBOARDING' END,
            0,
            CASE WHEN NEW.is_registration_completed AND copied_nickname IS NOT NULL
                THEN NEW.created_at - INTERVAL 9 HOUR END,
            NEW.created_at - INTERVAL 9 HOUR,
            NEW.updated_at - INTERVAL 9 HOUR);
    END IF;
END$$

-- 웹 가입은 소셜 로그인으로 회원을 먼저 만들고(가입 미완료) 닉네임을 정하면 가입 완료로 바꾼다.
CREATE TRIGGER trg_member_profile_after_update_complete_v2_user
AFTER UPDATE ON tb_member_profile
FOR EACH ROW
BEGIN
    IF NOT OLD.is_registration_completed AND NEW.is_registration_completed AND NEW.deleted_at IS NULL
        AND TRIM(NEW.nickname) REGEXP '^[가-힣a-zA-Z0-9_]{2,12}$'
        AND NOT EXISTS (
            SELECT 1 FROM tb_v2_user
            WHERE nickname_normalized = LOWER(TRIM(NEW.nickname)) AND id <> NEW.id) THEN
        UPDATE IGNORE tb_v2_user
        SET nickname = TRIM(NEW.nickname),
            nickname_normalized = LOWER(TRIM(NEW.nickname)),
            status = 'ACTIVE',
            onboarding_completed_at = NEW.updated_at - INTERVAL 9 HOUR,
            updated_at = NEW.updated_at - INTERVAL 9 HOUR
        WHERE id = NEW.id
            AND created_at = NEW.created_at - INTERVAL 9 HOUR
            AND status = 'ONBOARDING';
    END IF;
END$$

CREATE TRIGGER trg_social_account_after_insert_copy_v2_oauth
AFTER INSERT ON tb_social_account
FOR EACH ROW
BEGIN
    INSERT IGNORE INTO tb_v2_user_oauth_account (user_id, provider, provider_subject, provider_email, linked_at,
                                                 last_login_at)
    SELECT
        NEW.member_id,
        NEW.provider,
        NEW.provider_id,
        CASE WHEN CHAR_LENGTH(TRIM(NEW.email)) BETWEEN 1 AND 255 THEN TRIM(NEW.email) END,
        m.created_at - INTERVAL 9 HOUR,
        m.created_at - INTERVAL 9 HOUR
    FROM tb_member_profile m
    JOIN tb_v2_user u ON u.id = m.id AND u.created_at = m.created_at - INTERVAL 9 HOUR
    WHERE m.id = NEW.member_id
        AND m.deleted_at IS NULL
        AND NEW.provider IN ('KAKAO', 'GOOGLE', 'APPLE');
END$$

DELIMITER ;

-- 운영에서 87~92번을 수동으로 옮긴(2026-10-31) 뒤 이 마이그레이션이 배포되기 전까지 가입한 v1 회원을 옮긴다.
INSERT IGNORE INTO tb_v2_user (id, email, nickname, nickname_normalized, avatar_url, bio, role, status, log_count,
                               onboarding_completed_at, created_at, updated_at)
SELECT
    m.id,
    CASE WHEN CHAR_LENGTH(TRIM(m.email)) BETWEEN 1 AND 255 THEN TRIM(m.email) END,
    n.nickname,
    LOWER(n.nickname),
    NULLIF(TRIM(m.image_url), ''),
    NULLIF(m.bio, ''),
    'USER',
    CASE WHEN m.is_registration_completed AND n.nickname IS NOT NULL THEN 'ACTIVE' ELSE 'ONBOARDING' END,
    0,
    CASE WHEN m.is_registration_completed AND n.nickname IS NOT NULL THEN m.created_at - INTERVAL 9 HOUR END,
    m.created_at - INTERVAL 9 HOUR,
    m.updated_at - INTERVAL 9 HOUR
FROM tb_member_profile m
JOIN LATERAL (
    SELECT CASE
        WHEN TRIM(m.nickname) REGEXP '^[가-힣a-zA-Z0-9_]{2,12}$'
            AND NOT EXISTS (SELECT 1 FROM tb_v2_user u WHERE u.nickname_normalized = LOWER(TRIM(m.nickname)))
        THEN TRIM(m.nickname)
    END AS nickname
) n ON TRUE
WHERE m.id > 92
    AND m.deleted_at IS NULL
    AND NOT EXISTS (SELECT 1 FROM tb_v2_user u WHERE u.id = m.id);

INSERT IGNORE INTO tb_v2_user_oauth_account (user_id, provider, provider_subject, provider_email, linked_at,
                                             last_login_at)
SELECT
    s.member_id,
    s.provider,
    s.provider_id,
    CASE WHEN CHAR_LENGTH(TRIM(s.email)) BETWEEN 1 AND 255 THEN TRIM(s.email) END,
    m.created_at - INTERVAL 9 HOUR,
    m.created_at - INTERVAL 9 HOUR
FROM tb_social_account s
JOIN tb_member_profile m ON m.id = s.member_id
JOIN tb_v2_user u ON u.id = m.id AND u.created_at = m.created_at - INTERVAL 9 HOUR
WHERE s.member_id > 92
    AND m.deleted_at IS NULL
    AND s.provider IN ('KAKAO', 'GOOGLE', 'APPLE');
