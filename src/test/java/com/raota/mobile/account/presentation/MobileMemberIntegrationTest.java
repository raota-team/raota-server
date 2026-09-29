package com.raota.mobile.account.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class MobileMemberIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService accessTokens;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Set<Long> createdUserIds = new HashSet<>();

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
    }

    @AfterEach
    void cleanUp() {
        for (Long userId : createdUserIds) {
            jdbcTemplate.update("DELETE FROM tb_v2_user_consent WHERE user_id = ?", userId);
            users.deleteById(userId);
        }
        createdUserIds.clear();
    }

    @Test
    void 온보딩_회원은_닉네임이_없는_자신의_프로필을_조회한다() throws Exception {
        Long userId = createUser();

        mvc.perform(get("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.status").value("ONBOARDING"))
            .andExpect(jsonPath("$.data.nickname").value(nullValue()))
            .andExpect(jsonPath("$.data.email").value("member@example.com"));
    }

    @Test
    void 프로필의_네_필드만_수정하고_빈_문자열은_비우며_누락한_필드는_유지한다() throws Exception {
        Long userId = createUser();

        mvc.perform(patch("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                    """
                            {"email":"updated@example.com","avatarUrl":"https://example.com/a.png","bio":"좋은 라멘","favoriteRamenType":"돈코츠","status":"ACTIVE","nickname":"몰래변경"}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("updated@example.com"))
            .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/a.png"))
            .andExpect(jsonPath("$.data.bio").value("좋은 라멘"))
            .andExpect(jsonPath("$.data.favoriteRamenType").value("돈코츠"))
            .andExpect(jsonPath("$.data.status").value("ONBOARDING"))
            .andExpect(jsonPath("$.data.nickname").value(nullValue()));

        mvc.perform(patch("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"email":"","avatarUrl":"","bio":""}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(nullValue()))
            .andExpect(jsonPath("$.data.avatarUrl").value(nullValue()))
            .andExpect(jsonPath("$.data.bio").value(nullValue()))
            .andExpect(jsonPath("$.data.favoriteRamenType").value("돈코츠"));

        assertThat(users.findById(userId).orElseThrow().getStatus().name()).isEqualTo("ONBOARDING");
    }

    @Test
    void 잘못된_프로필_필드는_필드_오류와_함께_400이다() throws Exception {
        Long userId = createUser();

        for (String fieldAndValue : new String[] { "\"email\":\"invalid\"",
                "\"avatarUrl\":\"http://example.com/a.png\"", "\"bio\":\"" + "a".repeat(501) + "\"" }) {
            mvc.perform(patch("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + fieldAndValue + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
        }
    }

    @Test
    void 사용할_수_없는_회원은_프로필_조회와_수정을_거부한다() throws Exception {
        Long userId = createUser();
        jdbcTemplate.update("UPDATE tb_v2_user SET status = 'SUSPENDED' WHERE id = ?", userId);

        mvc.perform(get("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.message").value("사용할 수 없는 계정입니다."));
        mvc.perform(patch("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 인증하지_않은_프로필_요청은_v2_401_응답이다() throws Exception {
        mvc.perform(get("/api/v2/members/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(patch("/api/v2/members/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 닉네임_중복_확인은_표시값을_돌려주며_내_닉네임은_사용_가능하다() throws Exception {
        Long userId = createUser();
        Long otherId = createUser();
        jdbcTemplate.update("UPDATE tb_v2_user SET nickname = 'Ramen', nickname_normalized = 'ramen' WHERE id = ?",
                otherId);

        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "  Ｎｅｗ_９  ")
            .header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nickname").value("New_9"))
            .andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "rAmEn")
            .header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(false));
        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "ＲＡＭＥＮ")
            .header(HttpHeaders.AUTHORIZATION, authorization(otherId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void 유효하지_않은_닉네임은_400이고_익명_중복_조회는_401이다() throws Exception {
        Long userId = createUser();

        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "라멘🍜")
            .header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.message").value("닉네임은 2~12자의 한글·영문·숫자·밑줄만 쓸 수 있습니다."));
        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "라멘"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 온보딩을_마치면_활성_회원과_세_법적_동의_결정이_남는다() throws Exception {
        Long userId = createUser();
        String body = """
                {"nickname":"  ＲａＭｅＮ_９  ","consents":[
                {"type":"TERMS","documentVersion":"2026-09-01","granted":true},
                {"type":"PRIVACY","documentVersion":"2026-09-01","granted":true},
                {"type":"MARKETING","documentVersion":"2026-09-01","granted":false}]}
                """;

        mvc.perform(put("/api/v2/members/me/onboarding").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.nickname").value("RaMeN_9"));

        var user = users.findById(userId).orElseThrow();
        assertThat(user.getNicknameNormalized()).isEqualTo("ramen_9");
        assertThat(user.getOnboardingCompletedAt()).isNotNull();
        assertThat(jdbcTemplate.queryForList("SELECT consent_type FROM tb_v2_user_consent WHERE user_id = ?",
                String.class, userId))
            .containsExactlyInAnyOrder("TERMS", "PRIVACY", "MARKETING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_v2_user_consent WHERE user_id = ? AND document_version = '2026-09-01' AND decided_at IS NOT NULL",
                Integer.class, userId))
            .isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_v2_user_consent WHERE user_id = ? AND consent_type = 'MARKETING' AND granted = FALSE",
                Integer.class, userId))
            .isEqualTo(1);

        mvc.perform(put("/api/v2/members/me/onboarding").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("CONFLICT"))
            .andExpect(jsonPath("$.error.message").value("이미 온보딩을 마쳤습니다."));
    }

    @Test
    void 필수_동의가_빠지거나_거절되면_온보딩_상태와_동의_내역은_유지된다() throws Exception {
        Long userId = createUser();
        for (String body : new String[] { """
                {"nickname":"라멘왕","consents":[{"type":"TERMS","documentVersion":"2026-09-01","granted":true}]}
                """, """
                {"nickname":"라멘왕","consents":[
                {"type":"TERMS","documentVersion":"2026-09-01","granted":false},
                {"type":"PRIVACY","documentVersion":"2026-09-01","granted":true}]}
                """ }) {
            mvc.perform(put("/api/v2/members/me/onboarding").header(HttpHeaders.AUTHORIZATION, authorization(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        assertThat(users.findById(userId).orElseThrow().getStatus().name()).isEqualTo("ONBOARDING");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_v2_user_consent WHERE user_id = ?",
                Integer.class, userId))
            .isZero();
    }

    @Test
    void 중복된_동의_유형과_잘못된_문서_버전은_온보딩을_거절한다() throws Exception {
        Long userId = createUser();
        mvc.perform(put("/api/v2/members/me/onboarding").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"nickname":"라멘왕","consents":[
                    {"type":"TERMS","documentVersion":"2026-09-01","granted":true},
                    {"type":"TERMS","documentVersion":"2026-09-01","granted":true},
                    {"type":"PRIVACY","documentVersion":"2026-09-01","granted":true}]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mvc.perform(put("/api/v2/members/me/onboarding").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"nickname":"라멘왕","consents":[
                    {"type":"TERMS","documentVersion":"v1","granted":true},
                    {"type":"PRIVACY","documentVersion":"2026-09-01","granted":true}]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.fields[0].field").exists());
    }

    @Test
    void 다른_회원의_닉네임과_겹치면_409이고_익명_온보딩은_401이다() throws Exception {
        Long userId = createUser();
        Long otherId = createUser();
        jdbcTemplate.update("UPDATE tb_v2_user SET nickname = 'Ramen', nickname_normalized = 'ramen' WHERE id = ?",
                otherId);
        String body = """
                {"nickname":"  ＲＡＭＥＮ  ","consents":[
                {"type":"TERMS","documentVersion":"2026-09-01","granted":true},
                {"type":"PRIVACY","documentVersion":"2026-09-01","granted":true}]}
                """;
        mvc.perform(put("/api/v2/members/me/onboarding").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.message").value("이미 사용 중인 닉네임입니다."));
        mvc.perform(put("/api/v2/members/me/onboarding").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 동시에_선점된_닉네임의_데이터베이스_중복_오류도_409로_응답한다() throws Exception {
        Long userId = createUser();
        Long otherId = createUser();
        String body = """
                {"nickname":"Ramen","consents":[
                {"type":"TERMS","documentVersion":"2026-09-01","granted":true},
                {"type":"PRIVACY","documentVersion":"2026-09-01","granted":true}]}
                """;
        CountDownLatch nicknameReserved = new CountDownLatch(1);
        CountDownLatch commitReservation = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var reservation = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                jdbcTemplate.update(
                        "UPDATE tb_v2_user SET nickname = 'Ramen', nickname_normalized = 'ramen' WHERE id = ?",
                        otherId);
                nicknameReserved.countDown();
                try {
                    if (!commitReservation.await(15, TimeUnit.SECONDS)) {
                        throw new AssertionError("선점한 닉네임을 확정할 때까지 기다려야 합니다.");
                    }
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                return null;
            }));
            assertThat(nicknameReserved.await(10, TimeUnit.SECONDS)).isTrue();

            var onboarding = executor.submit(
                    () -> mvc
                        .perform(put("/api/v2/members/me/onboarding")
                            .header(HttpHeaders.AUTHORIZATION, authorization(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                        .andReturn()
                        .getResponse());
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (System.nanoTime() < deadline && jdbcTemplate
                    .queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class) == 0) {
                    TimeUnit.MILLISECONDS.sleep(20);
                }
                assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits",
                        Integer.class))
                    .isPositive();
            }
            finally {
                commitReservation.countDown();
            }
            reservation.get(10, TimeUnit.SECONDS);
            var response = onboarding.get(10, TimeUnit.SECONDS);
            assertThat(response.getStatus()).isEqualTo(409);
            assertThat(response.getContentAsString()).contains("\"code\":\"CONFLICT\"", "이미 사용 중인 닉네임입니다.");
        }
        assertThat(users.findById(userId).orElseThrow().getStatus().name()).isEqualTo("ONBOARDING");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_v2_user_consent WHERE user_id = ?",
                Integer.class, userId))
            .isZero();
    }

    private Long createUser() {
        Long userId = users.saveAndFlush(MobileUser.onboarding("member@example.com")).getId();
        createdUserIds.add(userId);
        return userId;
    }

    private String authorization(Long userId) {
        return "Bearer " + accessTokens.issue(userId);
    }

}
