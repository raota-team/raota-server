package com.raota.mobile.ramenlog.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class MobileRamenLogLikeIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900006401L;

    private static final long PUBLIC_LOG = 900006411L;

    private static final long PRIVATE_LOG = 900006412L;

    private static final long DELETED_LOG = 900006413L;

    private static final long WITHDRAWING_LOG = 900006414L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    private Long author;

    private Long liker;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter)
                .apply(springSecurity())
                .build();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, is_published, created_at, updated_at)
                VALUES (?, '공감 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        author = active();
        liker = active();
        MobileUser pending = MobileUser.onboarding("like-" + UUID.randomUUID() + "@example.com");
        pending.completeOnboarding(
                Nickname.of("L" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        pending.requestWithdrawal(Instant.now(), Duration.ofDays(30));
        Long withdrawing = users.saveAndFlush(pending).getId();
        created.add(withdrawing);
        insertLog(PUBLIC_LOG, author, "PUBLIC", false);
        insertLog(PRIVATE_LOG, author, "PRIVATE", false);
        insertLog(DELETED_LOG, author, "PUBLIC", true);
        insertLog(WITHDRAWING_LOG, withdrawing, "PUBLIC", false);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_ramen_log_like WHERE ramen_log_id BETWEEN ? AND ?", PUBLIC_LOG, WITHDRAWING_LOG);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE shop_id = ?", SHOP);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        created.forEach(users::deleteById);
    }

    @Test
    void 공감과_취소는_반복해도_한_번만_반영되고_상세의_공감_여부에_보인다() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(put("/api/v2/ramen-logs/{logId}/like", PUBLIC_LOG)
                            .header(HttpHeaders.AUTHORIZATION, auth(liker)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(true))
                    .andExpect(jsonPath("$.data.likeCount").value(1));
        }
        mvc.perform(get("/api/v2/ramen-logs/{logId}", PUBLIC_LOG).header(HttpHeaders.AUTHORIZATION, auth(liker)))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
        mvc.perform(get("/api/v2/ramen-logs/{logId}", PUBLIC_LOG).header(HttpHeaders.AUTHORIZATION, auth(author)))
                .andExpect(jsonPath("$.data.isLiked").value(false));
        mvc.perform(get("/api/v2/members/me/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(author)))
                .andExpect(jsonPath("$.data.items[?(@.id == '%d')].likeCount".formatted(PUBLIC_LOG))
                        .value(1));

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete("/api/v2/ramen-logs/{logId}/like", PUBLIC_LOG)
                            .header(HttpHeaders.AUTHORIZATION, auth(liker)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(false))
                    .andExpect(jsonPath("$.data.likeCount").value(0));
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_ramen_log_like WHERE ramen_log_id = ?", Integer.class, PUBLIC_LOG))
                .isZero();
    }

    @Test
    void 여러_회원이_동시에_같은_요청을_반복해도_공감_수는_회원_수와_같다() throws Exception {
        List<Long> likers = List.of(active(), active(), active(), active());
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (Long userId : likers) {
            for (int repeat = 0; repeat < 2; repeat++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return mvc.perform(put("/api/v2/ramen-logs/{logId}/like", PUBLIC_LOG)
                                    .header(HttpHeaders.AUTHORIZATION, auth(userId)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo(200);
        }
        executor.shutdown();

        assertThat(jdbc.queryForObject(
                        "SELECT like_count FROM tb_v2_ramen_log WHERE id = ?", Integer.class, PUBLIC_LOG))
                .isEqualTo(likers.size());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_ramen_log_like WHERE ramen_log_id = ?", Integer.class, PUBLIC_LOG))
                .isEqualTo(likers.size());
    }

    @Test
    void 볼_수_없는_기록은_404이고_비회원은_401_가입_중인_회원은_403이다() throws Exception {
        for (long logId : List.of(PRIVATE_LOG, DELETED_LOG, WITHDRAWING_LOG, 900006499L)) {
            mvc.perform(put("/api/v2/ramen-logs/{logId}/like", logId).header(HttpHeaders.AUTHORIZATION, auth(liker)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        }
        mvc.perform(put("/api/v2/ramen-logs/{logId}/like", PUBLIC_LOG)).andExpect(status().isUnauthorized());
        Long onboarding = users.saveAndFlush(
                        MobileUser.onboarding("like-onboarding-" + UUID.randomUUID() + "@example.com"))
                .getId();
        created.add(onboarding);
        mvc.perform(put("/api/v2/ramen-logs/{logId}/like", PUBLIC_LOG)
                        .header(HttpHeaders.AUTHORIZATION, auth(onboarding)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
        mvc.perform(put("/api/v2/ramen-logs/{logId}/like", PRIVATE_LOG).header(HttpHeaders.AUTHORIZATION, auth(author)))
                .andExpect(status().isOk());
    }

    private void insertLog(long id, Long userId, String visibility, boolean deleted) {
        String deletedAt = null;
        if (deleted) {
            deletedAt = "2026-09-02 00:00:00";
        }
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    satisfaction_score, broth_density_score, noodle_firmness_score, topping_score,
                    revisit_intention, note, taste_note_codes, visibility, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, '2026-09-01', '라멘', '쇼유', 4, 3, 3, 3, 'OFTEN', '', '[]', ?,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00', ?)
                """, id, userId, SHOP, visibility, deletedAt);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("like-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("L" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }
}
