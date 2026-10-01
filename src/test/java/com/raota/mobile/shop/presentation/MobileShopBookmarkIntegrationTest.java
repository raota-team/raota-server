package com.raota.mobile.shop.presentation;

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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 북마크 요청과 저장된 매장 목록이 회원별로 분리되는지 검증한다. */
class MobileShopBookmarkIntegrationTest extends BaseIntegrationTest {

    private static final long FIRST = 900003101L;

    private static final long SECOND = 900003102L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService accessTokens;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private Long activeId;

    private Long otherId;

    private Long onboardingId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        for (long id : List.of(FIRST, SECOND)) {
            jdbc.update(
                    """
                            INSERT INTO tb_v2_shop (id, name, address, ramen_types, tags, business_status,
                                ai_summary_keywords, view_count, log_count, bookmark_count, is_published, created_at, updated_at)
                            VALUES (?, '북마크 매장', '서울', '[]', '[]', 'OPERATIONAL', '[]', 0, 0, 0,
                                TRUE, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000')
                            """,
                    id);
        }
        activeId = active();
        otherId = active();
        onboardingId = users.saveAndFlush(MobileUser.onboarding("new@example.com")).getId();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop_bookmark WHERE shop_id IN (?, ?)", FIRST, SECOND);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id IN (?, ?)", FIRST, SECOND);
        for (Long userId : List.of(activeId, otherId, onboardingId)) {
            users.deleteById(userId);
        }
    }

    @Test
    void 같은_북마크를_두_번_저장하고_두_번_삭제해도_카운트는_각각_한_번만_변한다() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(put("/api/v2/shops/{shopId}/bookmark", FIRST).header(HttpHeaders.AUTHORIZATION,
                    authorization(activeId)))
                .andExpect(status().isNoContent());
        }
        assertThat(bookmarkCount(FIRST)).isEqualTo(1);
        assertThat(
                jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_bookmark WHERE shop_id = ?", Integer.class, FIRST))
            .isEqualTo(1);
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete("/api/v2/shops/{shopId}/bookmark", FIRST).header(HttpHeaders.AUTHORIZATION,
                    authorization(activeId)))
                .andExpect(status().isNoContent());
        }
        assertThat(bookmarkCount(FIRST)).isZero();
    }

    @Test
    void 동시에_두_번_저장해도_PK_충돌은_성공으로_처리하고_카운트는_하나다() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var save = (java.util.concurrent.Callable<Void>) () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("저장 요청 시작을 기다리지 못했습니다.");
                }
                add(FIRST, activeId);
                return null;
            };
            var first = executor.submit(save);
            var second = executor.submit(save);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        assertThat(bookmarkCount(FIRST)).isEqualTo(1);
        assertThat(
                jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_bookmark WHERE shop_id = ?", Integer.class, FIRST))
            .isEqualTo(1);
    }

    @Test
    void 비회원과_온보딩_회원은_북마크를_쓸_수_없고_없는_매장은_404이다() throws Exception {
        mvc.perform(put("/api/v2/shops/{shopId}/bookmark", FIRST))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(put("/api/v2/shops/{shopId}/bookmark", FIRST).header(HttpHeaders.AUTHORIZATION,
                authorization(onboardingId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
        mvc.perform(put("/api/v2/shops/{shopId}/bookmark", 900003999L).header(HttpHeaders.AUTHORIZATION,
                authorization(activeId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        jdbc.update("UPDATE tb_v2_shop SET is_published = FALSE WHERE id = ?", SECOND);
        mvc.perform(put("/api/v2/shops/{shopId}/bookmark", SECOND).header(HttpHeaders.AUTHORIZATION,
                authorization(activeId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(delete("/api/v2/shops/{shopId}/bookmark", 900003999L).header(HttpHeaders.AUTHORIZATION,
                authorization(activeId)))
            .andExpect(status().isNoContent());
    }

    @Test
    void 목록과_상세의_저장_상태는_소유자에게만_보인다() throws Exception {
        add(FIRST, activeId);
        assertThat(markedInList(activeId, FIRST)).isTrue();
        assertThat(markedInList(otherId, FIRST)).isFalse();
        assertThat(markedInList(null, FIRST)).isFalse();
        assertThat(detail(FIRST, activeId).get("isBookmarked").asBoolean()).isTrue();
        assertThat(detail(FIRST, otherId).get("isBookmarked").asBoolean()).isFalse();
        assertThat(detail(FIRST, null).get("isBookmarked").asBoolean()).isFalse();
    }

    @Test
    void 내_저장_목록은_저장_시각과_매장_ID_순서로_이어지고_타인은_볼_수_없다() throws Exception {
        add(FIRST, activeId);
        add(SECOND, activeId);
        JsonNode firstPage = bookmarked(activeId, null, 1);
        assertThat(firstPage.get("items").get(0).get("id").asString()).isEqualTo(Long.toString(SECOND));
        assertThat(firstPage.get("items").get(0).get("isBookmarked").asBoolean()).isTrue();
        assertThat(firstPage.get("hasNext").asBoolean()).isTrue();
        JsonNode next = bookmarked(activeId, firstPage.get("nextCursor").asString(), 1);
        assertThat(next.get("items").get(0).get("id").asString()).isEqualTo(Long.toString(FIRST));
        assertThat(next.get("hasNext").asBoolean()).isFalse();
        assertThat(bookmarked(otherId, null, 20).get("items").size()).isZero();
        assertThat(bookmarked(onboardingId, null, 20).get("items").size()).isZero();
        mvc.perform(get("/api/v2/members/me/bookmarked-shops"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private void add(long shopId, Long userId) throws Exception {
        mvc.perform(
                put("/api/v2/shops/{shopId}/bookmark", shopId).header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isNoContent());
    }

    private boolean markedInList(Long userId, long shopId) throws Exception {
        var request = get("/api/v2/shops");
        if (userId != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization(userId));
        }
        JsonNode items = mapper
            .readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray())
            .get("data")
            .get("items");
        for (JsonNode item : items) {
            if (item.get("id").asString().equals(Long.toString(shopId))) {
                return item.get("isBookmarked").asBoolean();
            }
        }
        throw new AssertionError("매장 목록에 기대한 매장이 없습니다.");
    }

    private JsonNode detail(long shopId, Long userId) throws Exception {
        var request = get("/api/v2/shops/{shopId}", shopId);
        if (userId != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization(userId));
        }
        return mapper
            .readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray())
            .get("data");
    }

    private JsonNode bookmarked(Long userId, String cursor, int size) throws Exception {
        var request = get("/api/v2/members/me/bookmarked-shops")
            .header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .param("size", Integer.toString(size));
        if (cursor != null) {
            request.param("cursor", cursor);
        }
        return mapper
            .readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray())
            .get("data");
    }

    private int bookmarkCount(long shopId) {
        return jdbc.queryForObject("SELECT bookmark_count FROM tb_v2_shop WHERE id = ?", Integer.class, shopId);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("member@example.com");
        user.completeOnboarding(Nickname.of("Bk" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        return users.saveAndFlush(user).getId();
    }

    private String authorization(Long userId) {
        return "Bearer " + accessTokens.issue(userId);
    }

}
