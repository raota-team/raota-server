package com.raota.mobile.ramenlog.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileLoungeSafetyIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900006601L;

    private static final long AUTHOR_LOG = 900006611L;

    private static final long OTHER_LOG = 900006612L;

    private static final long PRIVATE_LOG = 900006613L;

    private static final long REPORTER_LOG = 900006614L;

    private static final long COMMENT = 900006621L;

    private static final long DELETED_COMMENT = 900006622L;

    private static final long REPORTER_COMMENT = 900006623L;

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

    private final List<Long> created = new ArrayList<>();

    private Long author;

    private Long other;

    private Long reporter;

    private Long bystander;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter)
                .apply(springSecurity())
                .build();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, is_published, created_at, updated_at)
                VALUES (?, '신고 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        author = active();
        other = active();
        reporter = active();
        bystander = active();
        insertLog(AUTHOR_LOG, author, "PUBLIC", 1);
        insertLog(OTHER_LOG, other, "PUBLIC", 2);
        insertLog(PRIVATE_LOG, author, "PRIVATE", 3);
        insertLog(REPORTER_LOG, reporter, "PUBLIC", 4);
        insertComment(COMMENT, OTHER_LOG, author, 1, false);
        insertComment(DELETED_COMMENT, OTHER_LOG, author, 2, true);
        insertComment(REPORTER_COMMENT, OTHER_LOG, reporter, 3, false);
    }

    @AfterEach
    void cleanUp() {
        for (Long userId : created) {
            jdbc.update("DELETE FROM tb_v2_content_report WHERE reporter_id = ?", userId);
            jdbc.update("DELETE FROM tb_v2_user_block WHERE blocker_id = ?", userId);
        }
        jdbc.update("DELETE FROM tb_v2_ramen_log_comment WHERE ramen_log_id BETWEEN ? AND ?", AUTHOR_LOG, REPORTER_LOG);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE shop_id = ?", SHOP);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        created.forEach(users::deleteById);
    }

    @Test
    void 같은_대상을_다시_신고해도_한_건이고_신고한_기록과_댓글은_신고한_회원에게만_빠진다() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            report(reporter, "RAMEN_LOG", AUTHOR_LOG, "SPAM").andExpect(status().isNoContent());
        }
        report(reporter, "RAMEN_LOG_COMMENT", COMMENT, "ABUSE").andExpect(status().isNoContent());
        assertThat(jdbc.queryForList(
                        "SELECT CONCAT(target_type, ':', reason, ':', status) FROM tb_v2_content_report"
                                + " WHERE reporter_id = ? ORDER BY target_type",
                        String.class,
                        reporter))
                .containsExactly("RAMEN_LOG:SPAM:OPEN", "RAMEN_LOG_COMMENT:ABUSE:OPEN");

        assertThat(feedIds(reporter)).containsExactly(REPORTER_LOG, OTHER_LOG);
        assertThat(feedIds(bystander)).containsExactly(REPORTER_LOG, OTHER_LOG, AUTHOR_LOG);
        assertThat(feedIds(null)).containsExactly(REPORTER_LOG, OTHER_LOG, AUTHOR_LOG);
        assertThat(commentIds(reporter)).containsExactly(REPORTER_COMMENT);
        assertThat(commentIds(bystander)).containsExactly(COMMENT, REPORTER_COMMENT);
    }

    @Test
    void 자기_글과_볼_수_없는_대상과_잘못된_입력의_신고는_거절한다() throws Exception {
        report(reporter, "RAMEN_LOG", REPORTER_LOG, "SPAM").andExpect(status().isBadRequest());
        report(reporter, "RAMEN_LOG_COMMENT", REPORTER_COMMENT, "SPAM").andExpect(status().isBadRequest());
        report(reporter, "RAMEN_LOG", PRIVATE_LOG, "SPAM").andExpect(status().isNotFound());
        report(reporter, "RAMEN_LOG_COMMENT", DELETED_COMMENT, "SPAM").andExpect(status().isNotFound());
        report(reporter, "RAMEN_LOG", 900006699L, "SPAM").andExpect(status().isNotFound());
        for (String body : List.of(
                "{\"targetType\":\"SHOP\",\"targetId\":\"1\",\"reason\":\"SPAM\"}",
                "{\"targetType\":\"RAMEN_LOG\",\"targetId\":\"abc\",\"reason\":\"SPAM\"}",
                "{\"targetType\":\"RAMEN_LOG\",\"targetId\":\"" + AUTHOR_LOG + "\",\"reason\":\"BORING\"}",
                "{\"targetType\":\"RAMEN_LOG\",\"targetId\":\"" + AUTHOR_LOG + "\"}")) {
            mvc.perform(post("/api/v2/reports")
                            .header(HttpHeaders.AUTHORIZATION, auth(reporter))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_content_report WHERE reporter_id = ?", Integer.class, reporter))
                .isZero();
    }

    @Test
    void 차단한_회원의_기록과_댓글은_차단한_회원에게만_빠지고_해제하면_다시_보인다() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            block(reporter, author).andExpect(status().isNoContent());
        }
        block(reporter, other).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_user_block WHERE blocker_id = ?", Integer.class, reporter))
                .isEqualTo(2);

        assertThat(feedIds(reporter)).containsExactly(REPORTER_LOG);
        assertThat(feedIds(bystander)).containsExactly(REPORTER_LOG, OTHER_LOG, AUTHOR_LOG);
        JsonNode blocked =
                data(mvc.perform(get("/api/v2/members/me/blocks").header(HttpHeaders.AUTHORIZATION, auth(reporter)))
                        .andExpect(status().isOk()));
        assertThat(blocked.size()).isEqualTo(2);
        assertThat(blocked.get(0).get("id").asString()).isEqualTo(String.valueOf(other));
        assertThat(blocked.get(1).get("id").asString()).isEqualTo(String.valueOf(author));
        assertThat(blocked.get(1).has("nickname")).isTrue();
        assertThat(blocked.get(1).has("avatarUrl")).isTrue();

        block(reporter, other).andExpect(status().isNoContent());
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete("/api/v2/members/{memberId}/block", other)
                            .header(HttpHeaders.AUTHORIZATION, auth(reporter)))
                    .andExpect(status().isNoContent());
        }
        assertThat(feedIds(reporter)).containsExactly(REPORTER_LOG, OTHER_LOG);
        assertThat(commentIds(reporter)).containsExactly(REPORTER_COMMENT);
    }

    @Test
    void 활성_회원이_아닌_회원은_없는_회원과_같이_404이고_차단_목록에서도_빠진다() throws Exception {
        block(reporter, other).andExpect(status().isNoContent());
        block(reporter, author).andExpect(status().isNoContent());
        jdbc.update("UPDATE tb_v2_user SET status = 'WITHDRAW_PENDING' WHERE id = ?", other);

        block(bystander, other)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_user_block WHERE blocker_id = ?", Integer.class, bystander))
                .isZero();
        mvc.perform(get("/api/v2/members/me/blocks").header(HttpHeaders.AUTHORIZATION, auth(reporter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(String.valueOf(author)));
    }

    @Test
    void 자기_자신과_없는_회원은_차단할_수_없고_비회원과_가입_중인_회원은_쓰기를_할_수_없다() throws Exception {
        block(reporter, reporter).andExpect(status().isBadRequest());
        block(reporter, 900006698L).andExpect(status().isNotFound());

        mvc.perform(post("/api/v2/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"RAMEN_LOG\",\"targetId\":\"" + AUTHOR_LOG
                                + "\",\"reason\":\"SPAM\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v2/members/{memberId}/block", author)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v2/members/me/blocks")).andExpect(status().isUnauthorized());

        Long onboarding = users.saveAndFlush(
                        MobileUser.onboarding("safety-onboarding-" + UUID.randomUUID() + "@example.com"))
                .getId();
        created.add(onboarding);
        report(onboarding, "RAMEN_LOG", AUTHOR_LOG, "SPAM")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
        block(onboarding, author).andExpect(status().isForbidden());
        mvc.perform(get("/api/v2/members/me/blocks").header(HttpHeaders.AUTHORIZATION, auth(onboarding)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private ResultActions report(Long userId, String type, long targetId, String reason) throws Exception {
        return mvc.perform(post("/api/v2/reports")
                .header(HttpHeaders.AUTHORIZATION, auth(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"%s\",\"targetId\":\"%d\",\"reason\":\"%s\"}"
                        .formatted(type, targetId, reason)));
    }

    private ResultActions block(Long userId, long memberId) throws Exception {
        return mvc.perform(
                put("/api/v2/members/{memberId}/block", memberId).header(HttpHeaders.AUTHORIZATION, auth(userId)));
    }

    private List<Long> feedIds(Long viewer) throws Exception {
        var request = get("/api/v2/ramen-logs").param("shopId", String.valueOf(SHOP));
        if (viewer != null) {
            request.header(HttpHeaders.AUTHORIZATION, auth(viewer));
        }
        return ids(data(mvc.perform(request).andExpect(status().isOk())).get("items"));
    }

    private List<Long> commentIds(Long viewer) throws Exception {
        return ids(data(mvc.perform(get("/api/v2/ramen-logs/{logId}/comments", OTHER_LOG)
                                .header(HttpHeaders.AUTHORIZATION, auth(viewer)))
                        .andExpect(status().isOk()))
                .get("items"));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsByteArray())
                .get("data");
    }

    private List<Long> ids(JsonNode items) {
        List<Long> ids = new ArrayList<>();
        items.forEach(item -> ids.add(Long.valueOf(item.get("id").asString())));
        return ids;
    }

    private void insertLog(long id, Long userId, String visibility, int minute) {
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    satisfaction_score, broth_density_score, noodle_firmness_score, topping_score,
                    revisit_intention, note, taste_note_codes, visibility, created_at, updated_at)
                VALUES (?, ?, ?, '2026-09-01', '라멘', '쇼유', 4, 3, 3, 3, 'OFTEN', '', '[]', ?,
                    TIMESTAMPADD(MINUTE, ?, '2026-09-01 00:00:00'), '2026-09-01 00:00:00')
                """, id, userId, SHOP, visibility, minute);
    }

    private void insertComment(long id, long logId, Long userId, int minute, boolean deleted) {
        String deletedAt = null;
        if (deleted) {
            deletedAt = "2026-09-02 00:00:00";
        }
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log_comment (id, ramen_log_id, user_id, content, created_at, updated_at,
                    deleted_at)
                VALUES (?, ?, ?, '댓글', TIMESTAMPADD(MINUTE, ?, '2026-09-01 00:00:00'), '2026-09-01 00:00:00', ?)
                """, id, logId, userId, minute, deletedAt);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("safety-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(
                Nickname.of("S" + UUID.randomUUID().toString().substring(0, 8)), java.time.Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }
}
