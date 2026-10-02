package com.raota.mobile.ramenlog.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileRamenLogCrudIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900004701L;

    private static final long HIDDEN_SHOP = 900004702L;

    private static final String IMAGE = "https://mock.cdn.com/v2/ramen-logs/photo.png";

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

    private Long owner;

    private Long other;

    private Long onboarding;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        for (long shopId : List.of(SHOP, HIDDEN_SHOP)) {
            jdbc.update("""
                    INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                        ai_summary_keywords, is_published, created_at, updated_at)
                    VALUES (?, '테스트 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', ?,
                        '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                    """, shopId, shopId == SHOP);
        }
        owner = active();
        other = active();
        onboarding = users.saveAndFlush(MobileUser.onboarding("ramen-onboarding@example.com")).getId();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update(
                "DELETE FROM tb_v2_ramen_log_image WHERE ramen_log_id IN (SELECT id FROM tb_v2_ramen_log WHERE shop_id IN (?, ?))",
                SHOP, HIDDEN_SHOP);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE shop_id IN (?, ?)", SHOP, HIDDEN_SHOP);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id IN (?, ?)", SHOP, HIDDEN_SHOP);
        for (Long userId : List.of(owner, other, onboarding)) {
            users.deleteById(userId);
        }
    }

    @Test
    void 점수와_날짜와_사진과_매장_및_멱등키를_검증한다() throws Exception {
        for (String body : List.of(valid().replace("\"topping\":3", "\"topping\":null"),
                valid().replace("\"topping\":3", "\"topping\":6"),
                valid().replace("\"visitedAt\":\"2026-09-01\"", "\"visitedAt\":\"2999-01-01\""),
                valid().replace("[\"" + IMAGE + "\"]",
                        "[\"" + IMAGE + "\",\"" + IMAGE + "\",\"" + IMAGE + "\",\"" + IMAGE + "\"]"),
                valid().replace(IMAGE, "https://elsewhere.example/photo.png"),
                valid().replace("\"ramenType\":\"이에케\"", "\"ramenType\":\"우동\""),
                valid().replace("[\"BROTH_01\"]", "[\"BROTH_01\",\"BROTH_01\"]"),
                valid().replace("\"shopId\":\"" + SHOP + "\"", "\"shopId\":\"" + HIDDEN_SHOP + "\""))) {
            var response = mvc.perform(post("/api/v2/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(owner))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
            if (body.contains("\"shopId\":\"" + HIDDEN_SHOP + "\"")) {
                response.andExpect(status().isNotFound());
            }
            else {
                response.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
            }
        }
        mvc.perform(post("/api/v2/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(valid())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v2/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(owner))
            .header("Idempotency-Key", "bad key!")
            .contentType(MediaType.APPLICATION_JSON)
            .content(valid())).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log WHERE shop_id = ?", Integer.class, SHOP))
            .isZero();
    }

    @Test
    void 같은_키는_같은_기록을_돌려주고_동시_요청도_한_건만_작성한다() throws Exception {
        JsonNode first = create(owner, "repeat-key", valid());
        JsonNode second = create(owner, "repeat-key", valid());
        assertThat(second).isEqualTo(first);
        assertThat(first.get("ramenType").asString()).isEqualTo("이에케");
        assertThat(first.get("shop").get("id").asString()).isEqualTo(Long.toString(SHOP));
        assertThat(first.get("author").get("id").asString()).isEqualTo(owner.toString());
        assertThat(first.get("scores").get("revisit").asInt()).isEqualTo(5);
        assertThat(first.get("imageUrls").get(0).asString()).isEqualTo(IMAGE);
        assertThat(first.get("commentsPreview").size()).isZero();
        assertThat(first.get("commentsNextCursor").isNull()).isTrue();
        assertThat(first.get("createdAt").asString()).endsWith("Z");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var create = (java.util.concurrent.Callable<String>) () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("시작 신호가 없습니다.");
                }
                return create(owner, "concurrent-key", valid()).get("id").asString();
            };
            var left = pool.submit(create);
            var right = pool.submit(create);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(left.get(30, TimeUnit.SECONDS)).isEqualTo(right.get(30, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log WHERE shop_id = ?", Integer.class, SHOP))
            .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_user WHERE id = ?", Integer.class, owner))
            .isEqualTo(2);
    }

    @Test
    void 삭제한_기록의_멱등키를_다시_쓰면_충돌한다() throws Exception {
        String id = create(owner, "deleted-key", valid()).get("id").asString();
        mvc.perform(delete("/api/v2/ramen-logs/{logId}", id).header(HttpHeaders.AUTHORIZATION, auth(owner)))
            .andExpect(status().isNoContent());

        mvc.perform(post("/api/v2/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(owner))
            .header("Idempotency-Key", "deleted-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(valid()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("CONFLICT"))
            .andExpect(jsonPath("$.error.message").value("이미 삭제된 기록의 요청입니다."));
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_user WHERE id = ?", Integer.class, owner)).isZero();
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_shop WHERE id = ?", Integer.class, SHOP)).isZero();
    }

    @Test
    void 타인에게_비공개_기록과_탈퇴_유예_회원의_공개_기록은_보이지_않는다() throws Exception {
        String privateId = create(owner, "private", valid().replace("\"PUBLIC\"", "\"PRIVATE\"")).get("id").asString();
        mvc.perform(get("/api/v2/ramen-logs/{logId}", privateId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(get("/api/v2/ramen-logs/{logId}", privateId).header(HttpHeaders.AUTHORIZATION, auth(other)))
            .andExpect(status().isNotFound());
        assertThat(detail(privateId, owner).get("isMine").asBoolean()).isTrue();
        mvc.perform(patch("/api/v2/ramen-logs/{logId}", privateId).header(HttpHeaders.AUTHORIZATION, auth(other))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"note\":\"변경\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        String publicId = create(owner, "public", valid()).get("id").asString();
        MobileUser withdrawing = users.findById(owner).orElseThrow();
        withdrawing.requestWithdrawal(Instant.now(), Duration.ofDays(30));
        users.saveAndFlush(withdrawing);
        mvc.perform(get("/api/v2/ramen-logs/{logId}", publicId).header(HttpHeaders.AUTHORIZATION, auth(other)))
            .andExpect(status().isNotFound());
    }

    @Test
    void 기록을_작성한_뒤_매장이_숨김_처리되어도_상세와_멱등_재시도는_보인다() throws Exception {
        String id = create(owner, "hidden-after-create", valid()).get("id").asString();
        jdbc.update("UPDATE tb_v2_shop SET is_published = FALSE, deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", SHOP);
        assertThat(detail(id, other).get("shop").get("name").asString()).isEqualTo("테스트 매장");
        assertThat(create(owner, "hidden-after-create", valid()).get("id").asString()).isEqualTo(id);
    }

    @Test
    void 수정과_삭제는_사진과_카운터와_만족도_평균에_반영된다() throws Exception {
        assertThat(shopDetail().get("averageSatisfaction").isNull()).isTrue();
        String first = create(owner, "score-1", valid().replace("\"satisfaction\":5", "\"satisfaction\":1")).get("id")
            .asString();
        create(owner, "score-2", valid().replace("\"satisfaction\":5", "\"satisfaction\":4"));
        assertThat(shopDetail().get("averageSatisfaction").isNull()).isTrue();
        create(owner, "score-3", valid());
        assertThat(shopDetail().get("averageSatisfaction").decimalValue().toString()).isEqualTo("3.3");
        String fourth = create(owner, "score-4", valid().replace("\"satisfaction\":5", "\"satisfaction\":2")).get("id")
            .asString();
        JsonNode updated = mapper
            .readTree(mvc
                .perform(patch("/api/v2/ramen-logs/{logId}", fourth).header(HttpHeaders.AUTHORIZATION, auth(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"scores":{"satisfaction":4,"brothDensity":2,"noodleFirmness":4,"topping":3},
                            "imageUrls":[],"note":"  새 방문  ","visibility":"PRIVATE"}
                            """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray())
            .get("data");
        assertThat(updated.get("imageUrls").size()).isZero();
        assertThat(updated.get("note").asString()).isEqualTo("  새 방문  ");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log_image WHERE ramen_log_id = ?",
                Integer.class, Long.valueOf(fourth)))
            .isZero();
        assertThat(shopDetail().get("averageSatisfaction").decimalValue().toString()).isEqualTo("3.5");

        mvc.perform(delete("/api/v2/ramen-logs/{logId}", first).header(HttpHeaders.AUTHORIZATION, auth(owner)))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v2/ramen-logs/{logId}", first)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v2/ramen-logs/{logId}", first).header(HttpHeaders.AUTHORIZATION, auth(owner)))
            .andExpect(status().isNotFound());
        assertThat(shopDetail().get("averageSatisfaction").decimalValue().toString()).isEqualTo("4.3");
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_user WHERE id = ?", Integer.class, owner))
            .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_shop WHERE id = ?", Integer.class, SHOP))
            .isEqualTo(3);
    }

    @Test
    void 부분_수정도_입력_제약과_맛_태그_중복을_검증한다() throws Exception {
        String id = create(owner, "patch-validation", valid()).get("id").asString();
        for (String body : List.of("{\"menuName\":\"   \"}", "{\"note\":\"" + "x".repeat(501) + "\"}",
                "{\"imageUrls\":[\" \"]}", "{\"tasteNoteCodes\":[\"BROTH_01\",\"BROTH_01\"]}",
                "{\"scores\":{\"satisfaction\":null,\"brothDensity\":2,\"noodleFirmness\":4,\"topping\":3}}")) {
            mvc.perform(patch("/api/v2/ramen-logs/{logId}", id).header(HttpHeaders.AUTHORIZATION, auth(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        assertThat(detail(id, owner).get("menuName").asString()).isEqualTo("쇼유라멘");
    }

    @Test
    void 온보딩_회원은_작성할_수_없다() throws Exception {
        mvc.perform(post("/api/v2/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(onboarding))
            .header("Idempotency-Key", "onboarding")
            .contentType(MediaType.APPLICATION_JSON)
            .content(valid()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
    }

    private JsonNode create(Long userId, String key, String body) throws Exception {
        return mapper
            .readTree(mvc.perform(post("/api/v2/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(userId))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsByteArray())
            .get("data");
    }

    private JsonNode detail(String id, Long userId) throws Exception {
        return mapper
            .readTree(mvc.perform(get("/api/v2/ramen-logs/{logId}", id).header(HttpHeaders.AUTHORIZATION, auth(userId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray())
            .get("data");
    }

    private JsonNode shopDetail() throws Exception {
        return mapper
            .readTree(mvc.perform(get("/api/v2/shops/{shopId}", SHOP))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray())
            .get("data");
    }

    private String valid() {
        return """
                {"shopId":"%d","visitedAt":"2026-09-01","menuName":"  쇼유라멘  ","ramenType":"이에케",
                "scores":{"satisfaction":5,"brothDensity":2,"noodleFirmness":4,"topping":3},
                "revisitIntention":"OFTEN","note":"","tasteNoteCodes":["BROTH_01"],
                "visibility":"PUBLIC","imageUrls":["%s"]}
                """.formatted(SHOP, IMAGE);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("ramen-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("R" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        return users.saveAndFlush(user).getId();
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }

}
