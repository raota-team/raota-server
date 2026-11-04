package com.raota.mobile.ramenlog.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.mobile.ramenlog.application.service.MobileTasteReportService;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

class MobileTasteReportIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900015701L;

    private static final long OTHER_SHOP = 900015702L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService tokens;

    @Autowired
    private MobileTasteReportService reports;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private final List<Long> members = new ArrayList<>();

    private int keys;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter)
                .apply(springSecurity())
                .build();
        insertShop(SHOP, "이치니이치", "강남점");
        insertShop(OTHER_SHOP, "두번째집", null);
        jdbc.update(
                "INSERT INTO tb_v2_shop_image (shop_id, url, source, sort_order) VALUES (?, ?, 'ADMIN', 1)",
                SHOP,
                "https://images.raota.net/second.jpg");
        jdbc.update(
                "INSERT INTO tb_v2_shop_image (shop_id, url, source, sort_order) VALUES (?, ?, 'ADMIN', 0)",
                SHOP,
                "https://images.raota.net/first.jpg");
    }

    @AfterEach
    void cleanUp() {
        for (Long member : members) {
            jdbc.update("DELETE FROM tb_v2_taste_report WHERE user_id = ?", member);
            jdbc.update("DELETE FROM tb_v2_push_token WHERE user_id = ?", member);
            jdbc.update(
                    "DELETE FROM tb_v2_ramen_log_image WHERE ramen_log_id IN (SELECT id FROM tb_v2_ramen_log WHERE user_id = ?)",
                    member);
            jdbc.update("DELETE FROM tb_v2_ramen_log WHERE user_id = ?", member);
            users.deleteById(member);
        }
        jdbc.update("DELETE FROM tb_v2_shop_image WHERE shop_id IN (?, ?)", SHOP, OTHER_SHOP);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id IN (?, ?)", SHOP, OTHER_SHOP);
    }

    @Test
    void 세_번째_기록에서_첫_리포트를_뽑아_작성_응답과_조회에_굳힌_숫자를_준다() throws Exception {
        Long member = active();
        JsonNode first = create(member, SHOP, "2025-09-01", "쇼유", "라멘", 4);
        assertThat(first.get("drawnTasteReport").isNull()).isTrue();
        long legacy = insertUnscoredLog(member, OTHER_SHOP, "2025-08-01", "시오");
        String key = nextKey();
        JsonNode third = create(member, key, SHOP, "2025-09-03", "미소", "미소라멘", 5);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        JsonNode drawn = third.get("drawnTasteReport");
        assertThat(drawn.get("kind").asString()).isEqualTo("FIRST");
        assertThat(drawn.get("cutoffDate").asString()).isEqualTo(today.toString());
        assertThat(third.get("id").asString()).isNotBlank();
        assertThat(third.get("shop").get("id").asString()).isEqualTo(String.valueOf(SHOP));

        JsonNode replay = create(member, key, SHOP, "2025-09-03", "미소", "미소라멘", 5);
        assertThat(replay.get("id").asString()).isEqualTo(third.get("id").asString());
        assertThat(replay.get("drawnTasteReport").isNull()).isTrue();
        assertThat(reportCount(member)).isEqualTo(1);

        JsonNode current = current(member);
        JsonNode report = current.get("report");
        assertThat(report.get("id").asString()).isEqualTo(drawn.get("id").asString());
        assertThat(report.get("kind").asString()).isEqualTo("FIRST");
        assertThat(report.get("cutoffDate").asString()).isEqualTo(today.toString());
        assertThat(report.get("createdAt").asString()).endsWith("Z");
        assertThat(report.has("seenAt")).isTrue();
        assertThat(report.get("seenAt").isNull()).isTrue();
        assertThat(report.get("bowlCount").asInt()).isEqualTo(3);
        assertThat(report.get("shopCount").asInt()).isEqualTo(2);
        assertThat(mapper.convertValue(report.get("typeCounts"), Map.class))
                .isEqualTo(Map.of("쇼유", 1, "미소", 1, "시오", 1));

        // 점수 없는 옛 기록은 평균과 scoredCount에서 빠진다.
        JsonNode axes = report.get("axisAverages");
        assertThat(axes.get("scoredCount").asInt()).isEqualTo(2);
        assertThat(axes.get("satisfaction").asDouble()).isEqualTo(4.5);
        assertThat(axes.get("revisit").asDouble()).isEqualTo(3.0);

        JsonNode topShop = report.get("topShops").get(0);
        assertThat(topShop.get("shop").get("id").asString()).isEqualTo(String.valueOf(SHOP));
        assertThat(topShop.get("shop").get("name").asString()).isEqualTo("이치니이치");
        assertThat(topShop.get("shop").get("branchName").asString()).isEqualTo("강남점");
        assertThat(topShop.get("visitCount").asInt()).isEqualTo(2);
        // 메뉴가 한 번씩이면 최근 방문의 메뉴다.
        assertThat(topShop.get("topMenu").asString()).isEqualTo("미소라멘");
        assertThat(topShop.get("topRamenType").asString()).isEqualTo("미소");
        assertThat(topShop.get("lastVisitedAt").asString()).isEqualTo("2025-09-03");
        assertThat(topShop.get("photoUrl").asString()).isEqualTo("https://images.raota.net/first.jpg");
        JsonNode secondShop = report.get("topShops").get(1);
        assertThat(secondShop.get("shop").get("branchName").isNull()).isTrue();
        assertThat(secondShop.get("photoUrl").isNull()).isTrue();

        assertThat(texts(report.get("recentLogIds")))
                .containsExactly(third.get("id").asString(), first.get("id").asString(), String.valueOf(legacy));
        assertThat(current.get("upcoming").get("date").asString())
                .isEqualTo(today.withDayOfMonth(1).plusMonths(1).toString());
        assertThat(current.get("upcoming").get("needed").asInt()).isEqualTo(3);
    }

    @Test
    void 첫_리포트_전에는_리포트가_없고_남은_그릇_수만_준다() throws Exception {
        Long member = active();
        create(member, SHOP, "2025-09-01", "쇼유", "라멘", 4);

        JsonNode current = current(member);

        assertThat(current.get("report").isNull()).isTrue();
        assertThat(current.get("upcoming").get("date").isNull()).isTrue();
        assertThat(current.get("upcoming").get("needed").asInt()).isEqualTo(2);
    }

    @Test
    void 조회하면_빠진_매달_리포트를_뽑는다() throws Exception {
        Long member = active();
        insertReport(member, "2025-09-10");
        for (String date : List.of("2025-10-01", "2025-10-02", "2025-10-03")) {
            assertThat(create(member, SHOP, date, "쇼유", "라멘", 4)
                            .get("drawnTasteReport")
                            .isNull())
                    .isTrue();
        }

        JsonNode report = current(member).get("report");

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        assertThat(report.get("kind").asString()).isEqualTo("MONTHLY");
        assertThat(report.get("cutoffDate").asString())
                .isEqualTo(today.withDayOfMonth(1).minusDays(1).toString());
        assertThat(report.get("bowlCount").asInt()).isEqualTo(3);
        assertThat(reportCount(member)).isEqualTo(2);
    }

    @Test
    void 매달_배치는_모자라면_다음_달로_이어_세고_두_번_돌아도_하나만_뽑고_연말을_넘긴다() throws Exception {
        Long member = active();
        insertReport(member, "2025-10-15");
        create(member, SHOP, "2025-10-20", "쇼유", "라멘", 4);
        create(member, SHOP, "2025-10-25", "쇼유", "라멘", 4);

        reports.drawMonthlyForAll(LocalDate.of(2025, 11, 1));
        assertThat(cutoffs(member)).containsExactly("2025-10-15");

        create(member, OTHER_SHOP, "2025-11-10", "시오", "시오라멘", 3);
        reports.drawMonthlyForAll(LocalDate.of(2025, 12, 1));
        reports.drawMonthlyForAll(LocalDate.of(2025, 12, 1));
        assertThat(cutoffs(member)).containsExactly("2025-10-15", "2025-11-30");
        assertThat(bowlCount(member, "2025-11-30")).isEqualTo(3);

        for (String date : List.of("2025-12-01", "2025-12-15", "2025-12-31")) {
            create(member, SHOP, date, "쇼유", "라멘", 4);
        }
        reports.drawMonthlyForAll(LocalDate.of(2026, 1, 1));
        assertThat(cutoffs(member)).containsExactly("2025-10-15", "2025-11-30", "2025-12-31");
        assertThat(jdbc.queryForObject(
                        "SELECT kind FROM tb_v2_taste_report WHERE user_id = ? AND cutoff_date = '2025-12-31'",
                        String.class,
                        member))
                .isEqualTo("MONTHLY");
        // 리포트는 기준일까지의 기록을 모두 센다.
        assertThat(bowlCount(member, "2025-12-31")).isEqualTo(6);
    }

    @Test
    void 기준일_이전_날짜로_늦게_남긴_기록은_굳힌_숫자와_다음_뽑기_수에_들어가지_않는다() throws Exception {
        Long member = active();
        insertReport(member, "2025-09-30");
        for (String date : List.of("2025-10-01", "2025-10-02", "2025-10-03")) {
            create(member, SHOP, date, "쇼유", "라멘", 4);
        }
        reports.drawMonthlyForAll(LocalDate.of(2025, 11, 1));
        assertThat(bowlCount(member, "2025-10-31")).isEqualTo(3);

        create(member, OTHER_SHOP, "2025-10-10", "시오", "시오라멘", 2);
        JsonNode current = current(member);
        assertThat(current.get("report").get("cutoffDate").asString()).isEqualTo("2025-10-31");
        assertThat(current.get("report").get("bowlCount").asInt()).isEqualTo(3);
        assertThat(current.get("report").get("shopCount").asInt()).isEqualTo(1);

        create(member, SHOP, "2025-11-05", "쇼유", "라멘", 4);
        create(member, SHOP, "2025-11-06", "쇼유", "라멘", 4);
        reports.drawMonthlyForAll(LocalDate.of(2025, 12, 1));
        assertThat(cutoffs(member)).containsExactly("2025-09-30", "2025-10-31");
        assertThat(current(member).get("upcoming").get("needed").asInt()).isEqualTo(1);
    }

    @Test
    void 기록을_모두_지우면_리포트도_지워져_첫_리포트부터_다시_센다() throws Exception {
        Long member = active();
        List<String> ids = new ArrayList<>();
        for (String date : List.of("2025-09-01", "2025-09-02", "2025-09-03")) {
            ids.add(create(member, SHOP, date, "쇼유", "라멘", 4).get("id").asString());
        }
        assertThat(reportCount(member)).isEqualTo(1);

        deleteLog(member, ids.get(0));
        assertThat(reportCount(member)).isEqualTo(1);
        deleteLog(member, ids.get(1));
        deleteLog(member, ids.get(2));
        assertThat(reportCount(member)).isZero();

        JsonNode current = current(member);
        assertThat(current.get("report").isNull()).isTrue();
        assertThat(current.get("upcoming").get("needed").asInt()).isEqualTo(3);

        create(member, SHOP, "2025-10-01", "쇼유", "라멘", 4);
        create(member, SHOP, "2025-10-02", "쇼유", "라멘", 4);
        JsonNode third = create(member, SHOP, "2025-10-03", "쇼유", "라멘", 4);
        assertThat(third.get("drawnTasteReport").get("kind").asString()).isEqualTo("FIRST");
    }

    @Test
    void 방문일을_기준일_뒤로_옮겨_기준일까지의_기록이_없어지면_리포트를_지운다() throws Exception {
        Long member = active();
        insertReport(member, "2025-09-30");
        String moved =
                create(member, SHOP, "2025-09-15", "쇼유", "라멘", 4).get("id").asString();

        mvc.perform(patch("/api/v2/ramen-logs/{logId}", moved)
                        .header(HttpHeaders.AUTHORIZATION, auth(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitedAt\":\"2025-10-01\"}"))
                .andExpect(status().isOk());

        assertThat(reportCount(member)).isZero();
    }

    @Test
    void 처음_본_시각을_남기고_다시_불러도_그대로이며_남의_리포트는_찾을_수_없다() throws Exception {
        Long member = active();
        Long stranger = active();
        String reportId = drawFirst(member);

        JsonNode seen = data(seen(member, reportId).andExpect(status().isOk()));
        String seenAt = seen.get("seenAt").asString();
        assertThat(seenAt).endsWith("Z");
        assertThat(data(seen(member, reportId)).get("seenAt").asString()).isEqualTo(seenAt);
        assertThat(current(member).get("report").get("seenAt").asString()).isEqualTo(seenAt);

        seen(stranger, reportId).andExpect(status().isNotFound());
        seen(member, "999999999").andExpect(status().isNotFound());
    }

    private String drawFirst(Long member) throws Exception {
        create(member, SHOP, "2025-09-01", "쇼유", "라멘", 4);
        create(member, SHOP, "2025-09-02", "쇼유", "라멘", 4);
        return create(member, SHOP, "2025-09-03", "쇼유", "라멘", 4)
                .get("drawnTasteReport")
                .get("id")
                .asString();
    }

    private JsonNode create(Long member, long shopId, String date, String ramenType, String menu, int satisfaction)
            throws Exception {
        return create(member, nextKey(), shopId, date, ramenType, menu, satisfaction);
    }

    private JsonNode create(
            Long member, String key, long shopId, String date, String ramenType, String menu, int satisfaction)
            throws Exception {
        String body = """
                {"shopId":"%d","visitedAt":"%s","menuName":"%s","ramenType":"%s",
                "scores":{"satisfaction":%d,"brothDensity":3,"noodleFirmness":3,"topping":3},
                "revisitIntention":"SOMETIMES","note":"","tasteNoteCodes":[],"visibility":"PRIVATE","imageUrls":[]}
                """.formatted(shopId, date, menu, ramenType, satisfaction);
        return data(mvc.perform(post("/api/v2/ramen-logs")
                        .header(HttpHeaders.AUTHORIZATION, auth(member))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()));
    }

    private void deleteLog(Long member, String logId) throws Exception {
        mvc.perform(delete("/api/v2/ramen-logs/{logId}", logId).header(HttpHeaders.AUTHORIZATION, auth(member)))
                .andExpect(status().isNoContent());
    }

    private JsonNode current(Long member) throws Exception {
        return data(mvc.perform(
                        get("/api/v2/members/me/taste-reports/current").header(HttpHeaders.AUTHORIZATION, auth(member)))
                .andExpect(status().isOk()));
    }

    private ResultActions seen(Long member, String reportId) throws Exception {
        return mvc.perform(post("/api/v2/members/me/taste-reports/{reportId}/seen", reportId)
                .header(HttpHeaders.AUTHORIZATION, auth(member)));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsByteArray())
                .get("data");
    }

    private void insertShop(long id, String name, String branchName) {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, is_published, created_at, updated_at)
                VALUES (?, ?, ?, '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, id, name, branchName);
    }

    /** v1에서 옮겨 온 기록처럼 점수 없이 넣는다. */
    private long insertUnscoredLog(Long member, long shopId, String date, String ramenType) {
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (user_id, shop_id, visited_at, menu_name, ramen_type, note, taste_note_codes,
                    visibility, created_at, updated_at)
                VALUES (?, ?, ?, '옛 기록', ?, '', '[]', 'PRIVATE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, member, shopId, date, ramenType);
        return jdbc.queryForObject("SELECT MAX(id) FROM tb_v2_ramen_log WHERE user_id = ?", Long.class, member);
    }

    /** 이미 뽑은 리포트가 있는 회원을 만든다. 첫 리포트 기준일을 과거로 두어 매달 뽑기를 확인한다. */
    private void insertReport(Long member, String cutoff) {
        jdbc.update("""
                INSERT INTO tb_v2_taste_report (user_id, cutoff_date, kind, bowl_count, shop_count, type_counts,
                    axis_averages, top_shops, recent_log_ids, created_at)
                VALUES (?, ?, 'FIRST', 0, 0, '{}',
                    '{"scoredCount":0,"satisfaction":null,"brothDensity":null,"noodleFirmness":null,"topping":null,"revisit":null}',
                    '[]', '[]', CURRENT_TIMESTAMP(6))
                """, member, cutoff);
    }

    private int reportCount(Long member) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_taste_report WHERE user_id = ?", Integer.class, member);
    }

    private List<String> cutoffs(Long member) {
        return jdbc.queryForList(
                "SELECT CAST(cutoff_date AS CHAR) FROM tb_v2_taste_report WHERE user_id = ? ORDER BY cutoff_date",
                String.class,
                member);
    }

    private int bowlCount(Long member, String cutoff) {
        return jdbc.queryForObject(
                "SELECT bowl_count FROM tb_v2_taste_report WHERE user_id = ? AND cutoff_date = ?",
                Integer.class,
                member,
                cutoff);
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.asString());
        }
        return values;
    }

    private String nextKey() {
        return "taste-" + (++keys);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("taste-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("T" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        members.add(id);
        return id;
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }
}
