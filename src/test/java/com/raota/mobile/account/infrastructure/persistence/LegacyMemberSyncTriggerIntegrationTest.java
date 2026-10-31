package com.raota.mobile.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.support.BaseIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 웹(v1) 회원 가입이 DB 트리거(V45~V47)로 같은 ID의 앱(v2) 회원과 소셜 계정으로 복사된다. */
class LegacyMemberSyncTriggerIntegrationTest extends BaseIntegrationTest {

    private static final long MEMBER = 900009101L;

    private static final long OTHER_MEMBER = 900009102L;

    private static final LocalDateTime CREATED_KST = LocalDateTime.parse("2026-10-31T10:00:00");

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        for (long id : List.of(MEMBER, OTHER_MEMBER)) {
            jdbc.update("DELETE FROM tb_social_account WHERE member_id = ?", id);
            jdbc.update("DELETE FROM tb_member_profile WHERE id = ?", id);
            jdbc.update("DELETE FROM tb_v2_user_oauth_account WHERE user_id = ?", id);
            jdbc.update("DELETE FROM tb_v2_user WHERE id = ?", id);
        }
    }

    @Test
    void 가입을_마친_웹_회원은_같은_ID의_ACTIVE_앱_회원과_소셜_계정으로_복사되고_시각은_UTC다() {
        insertMember(MEMBER, "라멘러버", true);
        insertSocialAccount(MEMBER, "KAKAO", "kakao-901");

        Map<String, Object> user = v2User(MEMBER);
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat(user.get("nickname")).isEqualTo("라멘러버");
        assertThat(user.get("created_at")).isEqualTo(CREATED_KST.minusHours(9));
        assertThat(user.get("onboarding_completed_at")).isEqualTo(CREATED_KST.minusHours(9));
        assertThat(jdbc.queryForList(
                        "SELECT provider_subject FROM tb_v2_user_oauth_account WHERE user_id = ?",
                        String.class,
                        MEMBER))
                .containsExactly("kakao-901");
    }

    @Test
    void 가입을_마치기_전_닉네임은_복사하지_않고_가입을_마치면_띄어쓰기를_밑줄로_바꿔_ACTIVE가_된다() {
        insertMember(MEMBER, "윤영채", false);
        assertThat(v2User(MEMBER).get("status")).isEqualTo("ONBOARDING");
        assertThat(v2User(MEMBER).get("nickname")).isNull();

        jdbc.update(
                "UPDATE tb_member_profile SET nickname = ?, is_registration_completed = 1 WHERE id = ?",
                " 라멘오니 윤영채 ",
                MEMBER);

        assertThat(v2User(MEMBER).get("status")).isEqualTo("ACTIVE");
        assertThat(v2User(MEMBER).get("nickname")).isEqualTo("라멘오니_윤영채");
    }

    @Test
    void 자음이나_특수문자가_있는_닉네임은_그대로_옮긴다() {
        insertMember(MEMBER, "라ㄹ랜ㄷ?", true);

        assertThat(v2User(MEMBER).get("status")).isEqualTo("ACTIVE");
        assertThat(v2User(MEMBER).get("nickname")).isEqualTo("라ㄹ랜ㄷ?");
    }

    @Test
    void 가입을_마치면_앱에서_비어_있는_이메일과_소개를_채우고_앱에서_정한_사진은_덮어쓰지_않는다() {
        jdbc.update("""
                INSERT INTO tb_member_profile (id, nickname, email, is_registration_completed, created_at, updated_at)
                VALUES (?, '윤영채', '', 0, ?, ?)
                """, MEMBER, CREATED_KST, CREATED_KST);
        jdbc.update("UPDATE tb_v2_user SET avatar_url = 'https://app.example/avatar.webp' WHERE id = ?", MEMBER);

        jdbc.update("""
                UPDATE tb_member_profile
                SET nickname = '라멘오니', email = 'web@example.com', bio = '오니가 되어라.',
                    image_url = 'https://web.example/profile.webp', is_registration_completed = 1
                WHERE id = ?
                """, MEMBER);

        Map<String, Object> user = v2User(MEMBER);
        assertThat(user.get("nickname")).isEqualTo("라멘오니");
        assertThat(user.get("email")).isEqualTo("web@example.com");
        assertThat(user.get("bio")).isEqualTo("오니가 되어라.");
        assertThat(user.get("avatar_url")).isEqualTo("https://app.example/avatar.webp");
    }

    @Test
    void 앱에서_이미_쓰는_닉네임이면_비워_두고_온보딩으로_남긴다() {
        insertMember(OTHER_MEMBER, "같은이름", true);

        insertMember(MEMBER, "같은이름", true);

        assertThat(v2User(MEMBER).get("status")).isEqualTo("ONBOARDING");
        assertThat(v2User(MEMBER).get("nickname")).isNull();
    }

    @Test
    void 앱_회원이_이미_같은_ID를_쓰면_웹_가입은_성공하고_앱_회원과_소셜_계정은_건드리지_않는다() {
        jdbc.update("""
                INSERT INTO tb_v2_user (id, nickname, nickname_normalized, role, status, log_count, created_at, updated_at)
                VALUES (?, '앱회원', '앱회원', 'USER', 'ACTIVE', 0, '2026-01-01 00:00:00', '2026-01-01 00:00:00')
                """, MEMBER);

        insertMember(MEMBER, "웹회원", true);
        insertSocialAccount(MEMBER, "GOOGLE", "google-901");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_member_profile WHERE id = ?", Integer.class, MEMBER))
                .isEqualTo(1);
        assertThat(v2User(MEMBER).get("nickname")).isEqualTo("앱회원");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_user_oauth_account WHERE user_id = ?", Integer.class, MEMBER))
                .isZero();
    }

    private void insertMember(long id, String nickname, boolean registrationCompleted) {
        jdbc.update("""
                INSERT INTO tb_member_profile (id, nickname, email, is_registration_completed, created_at, updated_at)
                VALUES (?, ?, 'web@example.com', ?, ?, ?)
                """, id, nickname, registrationCompleted, CREATED_KST, CREATED_KST);
    }

    private void insertSocialAccount(long memberId, String provider, String providerId) {
        jdbc.update("""
                INSERT INTO tb_social_account (member_id, provider, provider_id, email, nickname)
                VALUES (?, ?, ?, 'web@example.com', 'social')
                """, memberId, provider, providerId);
    }

    private Map<String, Object> v2User(long id) {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT status, nickname, email, bio, avatar_url, created_at, onboarding_completed_at
                FROM tb_v2_user WHERE id = ?
                """, id);
        row.replaceAll((column, value) -> {
            if (value instanceof java.sql.Timestamp timestamp) {
                return timestamp.toLocalDateTime();
            }
            return value;
        });
        return row;
    }
}
