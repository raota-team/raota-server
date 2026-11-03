package com.raota.mobile.account.presentation.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 관리자 웹의 앱 회원 조회: 닉네임·이메일·ID 검색, 상태 필터, 로그인 수단과 마지막 로그인 모음, 상세의 연결 계정과 제공자 식별자 비노출을 확인한다.
 */
class MobileUserAdminIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/admin/api/v2/users";

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    /** 다른 테스트가 남긴 회원과 섞이지 않게 이 테스트의 회원만 찾는 표식이다. */
    private static final String TAG = "usradm7q";

    private static final long ACTIVE = 900007101L;

    private static final long PENDING = 900007102L;

    private static final long ONBOARDING = 900007103L;

    private static final long MISSING = 900007199L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        jdbc.update(
                """
                INSERT INTO tb_v2_user (id, email, nickname, nickname_normalized, avatar_url, bio, favorite_ramen_type,
                    role, status, log_count, onboarding_completed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'https://images.raota.net/avatar.webp', '라멘 좋아요', '쇼유', 'USER', 'ACTIVE', 3,
                    ?, ?, ?)
                """,
                ACTIVE,
                "Active." + TAG + "@Example.com",
                "Ramen" + TAG,
                "ramen" + TAG,
                at("2026-09-01T01:00:00Z"),
                at("2026-09-01T00:00:00Z"),
                at("2026-09-02T00:00:00Z"));
        jdbc.update(
                """
                INSERT INTO tb_v2_user (id, email, nickname, nickname_normalized, role, status, log_count,
                    onboarding_completed_at, withdrawal_requested_at, purge_scheduled_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'USER', 'WITHDRAW_PENDING', 0, ?, ?, ?, ?, ?)
                """,
                PENDING,
                "pending@" + TAG + ".net",
                "Leaving" + TAG,
                "leaving" + TAG,
                at("2026-09-02T01:00:00Z"),
                at("2026-09-10T00:00:00Z"),
                at("2026-10-10T00:00:00Z"),
                at("2026-09-02T00:00:00Z"),
                at("2026-09-10T00:00:00Z"));
        jdbc.update("""
                INSERT INTO tb_v2_user (id, email, role, status, log_count, created_at, updated_at)
                VALUES (?, ?, 'USER', 'ONBOARDING', 0, ?, ?)
                """, ONBOARDING, "new@" + TAG + ".net", at("2026-09-03T00:00:00Z"), at("2026-09-03T00:00:00Z"));
        oauth(ACTIVE, "GOOGLE", "google-subject-" + TAG, "g@" + TAG + ".net", "2026-09-05", "2026-10-01");
        oauth(ACTIVE, "KAKAO", "kakao-subject-" + TAG, null, "2026-09-01", "2026-10-03");
        oauth(PENDING, "APPLE", "apple-subject-" + TAG, "a@" + TAG + ".net", "2026-09-02", "2026-09-09");
        jdbc.update(
                "UPDATE tb_v2_user_oauth_account SET apple_refresh_token_encrypted = 'secret-token' WHERE user_id = ?",
                PENDING);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_user_oauth_account WHERE user_id BETWEEN ? AND ?", ACTIVE, MISSING);
        jdbc.update("DELETE FROM tb_v2_user WHERE id BETWEEN ? AND ?", ACTIVE, MISSING);
    }

    @Test
    void 목록은_최근_가입순이고_로그인_수단과_마지막_로그인을_모아_보여준다() throws Exception {
        mvc.perform(byKeyword(TAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.page.totalElements").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(ONBOARDING)))
                .andExpect(jsonPath("$.data.items[0].nickname").isEmpty())
                .andExpect(jsonPath("$.data.items[0].status").value("ONBOARDING"))
                .andExpect(jsonPath("$.data.items[0].providers.length()").value(0))
                .andExpect(jsonPath("$.data.items[0].lastLoginAt").isEmpty())
                .andExpect(jsonPath("$.data.items[0].onboardingCompletedAt").isEmpty())
                .andExpect(jsonPath("$.data.items[1].id").value(String.valueOf(PENDING)))
                .andExpect(jsonPath("$.data.items[1].status").value("WITHDRAW_PENDING"))
                .andExpect(jsonPath("$.data.items[1].providers[0]").value("APPLE"))
                .andExpect(jsonPath("$.data.items[1].withdrawalRequestedAt").value("2026-09-10T00:00:00Z"))
                .andExpect(jsonPath("$.data.items[1].purgeScheduledAt").value("2026-10-10T00:00:00Z"))
                .andExpect(jsonPath("$.data.items[2].id").value(String.valueOf(ACTIVE)))
                .andExpect(jsonPath("$.data.items[2].nickname").value("Ramen" + TAG))
                .andExpect(jsonPath("$.data.items[2].email").value("Active." + TAG + "@Example.com"))
                .andExpect(jsonPath("$.data.items[2].role").value("USER"))
                .andExpect(jsonPath("$.data.items[2].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[2].providers.length()").value(2))
                .andExpect(jsonPath("$.data.items[2].providers[0]").value("KAKAO"))
                .andExpect(jsonPath("$.data.items[2].providers[1]").value("GOOGLE"))
                .andExpect(jsonPath("$.data.items[2].logCount").value(3))
                .andExpect(jsonPath("$.data.items[2].createdAt").value("2026-09-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.items[2].onboardingCompletedAt").value("2026-09-01T01:00:00Z"))
                .andExpect(jsonPath("$.data.items[2].lastLoginAt").value("2026-10-03T00:00:00Z"))
                .andExpect(jsonPath("$.data.items[2].withdrawalRequestedAt").isEmpty())
                .andExpect(jsonPath("$.data.items[2].avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.data.items[2].oauthAccounts").doesNotExist());

        mvc.perform(byKeyword(TAG).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(ACTIVE)))
                .andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.hasPrevious").value(true));
    }

    @Test
    void 키워드는_닉네임과_이메일을_대소문자_없이_찾고_숫자면_ID도_찾는다() throws Exception {
        mvc.perform(byKeyword("  RAMEN" + TAG.toUpperCase() + " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(ACTIVE)));
        mvc.perform(byKeyword("active." + TAG + "@example"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(ACTIVE)));
        mvc.perform(byKeyword("new@" + TAG))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(ONBOARDING)));
        mvc.perform(byKeyword(String.valueOf(PENDING)))
                .andExpect(jsonPath("$.data.items[?(@.id == '%s')]".formatted(PENDING))
                        .exists())
                .andExpect(jsonPath("$.data.items[?(@.id == '%s')]".formatted(ACTIVE))
                        .doesNotExist());
        mvc.perform(byKeyword("99999999999999999999999")).andExpect(status().isOk());
        mvc.perform(byKeyword("가".repeat(101))).andExpect(status().isBadRequest());
    }

    @Test
    void 상태로_거른다() throws Exception {
        mvc.perform(byKeyword(TAG).param("status", "WITHDRAW_PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(PENDING)));
        mvc.perform(byKeyword(TAG).param("status", "SUSPENDED"))
                .andExpect(jsonPath("$.data.items.length()").value(0));
        mvc.perform(get(BASE).param("status", "ACTIVE").param("size", "100").with(ADMIN))
                .andExpect(jsonPath("$.data.items[?(@.id == '%s')]".formatted(ACTIVE))
                        .exists())
                .andExpect(jsonPath("$.data.items[?(@.status != 'ACTIVE')]").isEmpty());
        mvc.perform(get(BASE).param("status", "UNKNOWN").with(ADMIN)).andExpect(status().isBadRequest());
    }

    @Test
    void 상세는_프로필과_연결_계정을_보여주고_제공자_식별자와_토큰은_내보내지_않는다() throws Exception {
        mvc.perform(get(BASE + "/" + ACTIVE).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(ACTIVE)))
                .andExpect(jsonPath("$.data.nickname").value("Ramen" + TAG))
                .andExpect(jsonPath("$.data.providers[0]").value("KAKAO"))
                .andExpect(jsonPath("$.data.providers[1]").value("GOOGLE"))
                .andExpect(jsonPath("$.data.lastLoginAt").value("2026-10-03T00:00:00Z"))
                .andExpect(jsonPath("$.data.logCount").value(3))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://images.raota.net/avatar.webp"))
                .andExpect(jsonPath("$.data.bio").value("라멘 좋아요"))
                .andExpect(jsonPath("$.data.favoriteRamenType").value("쇼유"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-09-02T00:00:00Z"))
                .andExpect(jsonPath("$.data.oauthAccounts.length()").value(2))
                .andExpect(jsonPath("$.data.oauthAccounts[0].provider").value("KAKAO"))
                .andExpect(jsonPath("$.data.oauthAccounts[0].email").isEmpty())
                .andExpect(jsonPath("$.data.oauthAccounts[0].linkedAt").value("2026-09-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.oauthAccounts[0].lastLoginAt").value("2026-10-03T00:00:00Z"))
                .andExpect(jsonPath("$.data.oauthAccounts[1].provider").value("GOOGLE"))
                .andExpect(jsonPath("$.data.oauthAccounts[1].email").value("g@" + TAG + ".net"))
                .andExpect(jsonPath("$.data.oauthAccounts[1].linkedAt").value("2026-09-05T00:00:00Z"))
                .andExpect(jsonPath("$.data.oauthAccounts[1].lastLoginAt").value("2026-10-01T00:00:00Z"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("subject"))));
        mvc.perform(get(BASE + "/" + PENDING).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.oauthAccounts[0].provider").value("APPLE"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("secret-token"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("RefreshToken"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("subject"))));
        mvc.perform(get(BASE + "/" + ONBOARDING).with(ADMIN))
                .andExpect(jsonPath("$.data.oauthAccounts.length()").value(0))
                .andExpect(jsonPath("$.data.lastLoginAt").isEmpty());
    }

    @Test
    void 없는_회원은_찾을_수_없다() throws Exception {
        mvc.perform(get(BASE + "/" + MISSING).with(ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("회원을 찾을 수 없습니다."));
    }

    @Test
    void 웹_관리자만_쓸_수_있다() throws Exception {
        RequestPostProcessor member = user("member").roles("USER");
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/" + ACTIVE)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).with(member)).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/" + ACTIVE).with(member)).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder byKeyword(String keyword) {
        return get(BASE).param("keyword", keyword).with(ADMIN);
    }

    /** 날짜는 그날 UTC 자정이다. */
    private void oauth(
            long userId, String provider, String subject, String email, String linkedOn, String lastLoginOn) {
        jdbc.update("""
                INSERT INTO tb_v2_user_oauth_account (user_id, provider, provider_subject, provider_email, linked_at,
                    last_login_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, provider, subject, email, at(linkedOn + "T00:00:00Z"), at(lastLoginOn + "T00:00:00Z"));
    }

    /** v2 테이블은 UTC로 저장하므로 JVM 시간대와 상관없이 UTC 벽시계 값으로 넣는다. */
    private static String at(String instant) {
        return Instant.parse(instant).toString().replace('T', ' ').replace("Z", "");
    }
}
