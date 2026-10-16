package com.raota.mobile.ramenlog.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileRamenLogFeedIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900006301L;

    private static final long LOG = 900006310L;

    private static final Instant BASE = Instant.parse("2026-09-01T00:00:00Z");

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

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private Long author;

    private Long withdrawing;

    private Long viewer;

    private Long admin;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, is_published, created_at, updated_at)
                VALUES (?, '피드 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        author = active();
        viewer = active();
        admin = active();
        jdbc.update("UPDATE tb_v2_user SET role = 'ADMIN' WHERE id = ?", admin);
        MobileUser pending = MobileUser.onboarding("feed-" + UUID.randomUUID() + "@example.com");
        pending.completeOnboarding(Nickname.of("F" + UUID.randomUUID().toString().substring(0, 8)), BASE);
        pending.requestWithdrawal(BASE, Duration.ofDays(30));
        withdrawing = users.saveAndFlush(pending).getId();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_ramen_log_like WHERE ramen_log_id BETWEEN ? AND ?", LOG, LOG + 99);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE shop_id = ?", SHOP);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        for (Long userId : List.of(author, withdrawing, viewer, admin)) {
            users.deleteById(userId);
        }
    }

    @Test
    void 최신순_피드는_공개된_활성_회원의_기록만_보여주고_걸러낸_만큼_더_읽어_페이지를_채운다() throws Exception {
        insertLog(LOG + 1, author, "쇼유", "PUBLIC", 0, 1, false);
        insertLog(LOG + 2, withdrawing, "쇼유", "PUBLIC", 0, 2, false);
        insertLog(LOG + 3, withdrawing, "쇼유", "PUBLIC", 0, 3, false);
        insertLog(LOG + 4, author, "쇼유", "PRIVATE", 0, 4, false);
        insertLog(LOG + 5, author, "쇼유", "PUBLIC", 0, 5, true);
        insertLog(LOG + 6, author, "쇼유", "PUBLIC", 0, 6, false);
        insertLog(LOG + 7, withdrawing, "쇼유", "PUBLIC", 0, 7, false);
        insertLog(LOG + 8, author, "쇼유", "PUBLIC", 0, 8, false);

        JsonNode first = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP)).param("size", "2"));
        assertThat(ids(first)).containsExactly(LOG + 8, LOG + 6);
        assertThat(first.get("hasNext").asBoolean()).isTrue();

        JsonNode second = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))
            .param("size", "2")
            .param("cursor", first.get("nextCursor").asString()));
        assertThat(ids(second)).containsExactly(LOG + 1);
        assertThat(second.get("hasNext").asBoolean()).isFalse();
        assertThat(second.get("nextCursor").isNull()).isTrue();
    }

    @Test
    void 걸러낼_기록이_많으면_한_요청에서_묶음_상한까지만_읽고_빈_페이지와_이어_읽을_커서를_돌려준다() throws Exception {
        insertLog(LOG + 40, author, "쇼유", "PUBLIC", 0, 1, false);
        for (int minute = 2; minute <= 13; minute++) {
            insertLog(LOG + 40 + minute, withdrawing, "쇼유", "PUBLIC", 0, minute, false);
        }

        JsonNode first = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP)).param("size", "1"));
        assertThat(ids(first)).isEmpty();
        assertThat(first.get("hasNext").asBoolean()).isTrue();

        JsonNode second = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))
            .param("size", "1")
            .param("cursor", first.get("nextCursor").asString()));
        assertThat(ids(second)).containsExactly(LOG + 40);
        assertThat(second.get("hasNext").asBoolean()).isFalse();
    }

    @Test
    void 공감순은_공감_수와_ID_역순으로_이어지고_라멘_종류로_거른다() throws Exception {
        insertLog(LOG + 11, author, "쇼유", "PUBLIC", 5, 1, false);
        insertLog(LOG + 12, author, "쇼유", "PUBLIC", 9, 2, false);
        insertLog(LOG + 13, author, "돈코츠", "PUBLIC", 7, 3, false);
        insertLog(LOG + 14, author, "쇼유", "PUBLIC", 5, 4, false);

        JsonNode first = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))
            .param("sort", "LIKES")
            .param("size", "2"));
        assertThat(ids(first)).containsExactly(LOG + 12, LOG + 13);
        JsonNode second = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))
            .param("sort", "LIKES")
            .param("size", "2")
            .param("cursor", first.get("nextCursor").asString()));
        assertThat(ids(second)).containsExactly(LOG + 14, LOG + 11);
        assertThat(second.get("hasNext").asBoolean()).isFalse();

        JsonNode shoyu = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))
            .param("sort", "LIKES")
            .param("ramenType", "쇼유"));
        assertThat(ids(shoyu)).containsExactly(LOG + 12, LOG + 14, LOG + 11);
    }

    @Test
    void 비회원과_회원과_관리자_모두_읽을_수_있고_회원별_공감_여부와_내_기록_여부가_표시된다() throws Exception {
        insertLog(LOG + 21, author, "쇼유", "PUBLIC", 0, 1, false);
        mvc.perform(put("/api/v2/ramen-logs/{logId}/like", LOG + 21).header(HttpHeaders.AUTHORIZATION, auth(viewer)))
            .andExpect(status().isOk());

        JsonNode anonymous = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))).get("items").get(0);
        assertThat(anonymous.get("likeCount").asInt()).isEqualTo(1);
        assertThat(anonymous.get("isLiked").asBoolean()).isFalse();
        assertThat(anonymous.get("isMine").asBoolean()).isFalse();

        JsonNode liker = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))
            .header(HttpHeaders.AUTHORIZATION, auth(viewer))).get("items").get(0);
        assertThat(liker.get("isLiked").asBoolean()).isTrue();
        assertThat(liker.get("isMine").asBoolean()).isFalse();

        JsonNode own = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))
            .header(HttpHeaders.AUTHORIZATION, auth(author))).get("items").get(0);
        assertThat(own.get("isLiked").asBoolean()).isFalse();
        assertThat(own.get("isMine").asBoolean()).isTrue();

        JsonNode byAdmin = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP))
            .header(HttpHeaders.AUTHORIZATION, auth(admin))).get("items").get(0);
        assertThat(byAdmin.get("id").asString()).isEqualTo(String.valueOf(LOG + 21));
    }

    @Test
    void 정렬과_맞지_않는_커서와_알_수_없는_정렬은_400이다() throws Exception {
        insertLog(LOG + 31, author, "쇼유", "PUBLIC", 0, 1, false);
        insertLog(LOG + 32, author, "쇼유", "PUBLIC", 0, 2, false);
        JsonNode latest = feed(get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP)).param("size", "1"));

        mvc.perform(
                get("/api/v2/ramen-logs").param("sort", "LIKES").param("cursor", latest.get("nextCursor").asString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v2/ramen-logs").param("sort", "POPULAR"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private JsonNode feed(MockHttpServletRequestBuilder request) throws Exception {
        return mapper
            .readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray())
            .get("data");
    }

    private List<Long> ids(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.get("items").forEach(item -> ids.add(Long.valueOf(item.get("id").asString())));
        return ids;
    }

    private void insertLog(long id, Long userId, String ramenType, String visibility, int likes, int minute,
            boolean deleted) {
        Timestamp createdAt = Timestamp.from(BASE.plus(Duration.ofMinutes(minute)));
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    satisfaction_score, broth_density_score, noodle_firmness_score, topping_score,
                    revisit_intention, note, taste_note_codes, visibility, like_count, created_at, updated_at,
                    deleted_at)
                VALUES (?, ?, ?, '2026-09-01', '라멘', ?, 4, 3, 3, 3, 'OFTEN', '', '[]', ?, ?, ?, ?, ?)
                """, id, userId, SHOP, ramenType, visibility, likes, createdAt, createdAt, deleted ? createdAt : null);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("feed-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("F" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        return users.saveAndFlush(user).getId();
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }

}
