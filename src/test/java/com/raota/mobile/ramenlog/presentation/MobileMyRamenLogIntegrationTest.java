package com.raota.mobile.ramenlog.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileMyRamenLogIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900004751L;

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

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, is_published, created_at, updated_at)
                VALUES (?, '방문 매장', '강남점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        owner = active();
        other = active();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update(
                "DELETE FROM tb_v2_ramen_log_image WHERE ramen_log_id IN (SELECT id FROM tb_v2_ramen_log WHERE shop_id = ?)",
                SHOP);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE shop_id = ?", SHOP);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        users.deleteById(owner);
        users.deleteById(other);
    }

    @Test
    void 비공개_기록을_포함하고_삭제와_타인의_기록을_빼며_날짜와_ID_순으로_페이지를_잇는다() throws Exception {
        String older = create(owner, "first", "2026-09-01", "PRIVATE");
        String second = create(owner, "second", "2026-09-02", "PUBLIC");
        String latest = create(owner, "third", "2026-09-02", "PRIVATE");
        String deleted = create(owner, "deleted", "2026-09-03", "PUBLIC");
        String stranger = create(other, "other", "2026-09-04", "PUBLIC");
        mvc.perform(delete("/api/v2/ramen-logs/{logId}", deleted).header(HttpHeaders.AUTHORIZATION, auth(owner)))
            .andExpect(status().isNoContent());
        jdbc.update("UPDATE tb_v2_shop SET is_published = FALSE, deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", SHOP);
        mvc.perform(get("/api/v2/ramen-logs/{logId}", latest).header(HttpHeaders.AUTHORIZATION, auth(owner)))
            .andExpect(status().isOk());

        List<String> ids = new ArrayList<>();
        String cursor = null;
        do {
            JsonNode page = page(cursor, 1);
            for (JsonNode item : page.get("items")) {
                ids.add(item.get("id").asString());
                assertThat(item.get("isMine").asBoolean()).isTrue();
                assertThat(item.get("imageUrls").get(0).asString()).startsWith("https://mock.cdn.com/");
                assertThat(item.get("shop").get("branchName").asString()).isEqualTo("강남점");
                assertThat(item.get("author").get("id").asString()).isEqualTo(owner.toString());
            }
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asString();
            assertThat(page.get("hasNext").asBoolean()).isEqualTo(cursor != null);
        }
        while (cursor != null);
        assertThat(ids).containsExactly(latest, second, older).doesNotContain(deleted, stranger);

        JsonNode summary = data("/api/v2/members/me/ramen-logs/summary");
        assertThat(summary.size()).isEqualTo(3);
        assertThat(summary.get(0).get("id").asString()).isEqualTo(latest);
        assertThat(summary.get(2).get("id").asString()).isEqualTo(older);
        assertThat(summary.get(0).get("scores").get("revisit").asInt()).isEqualTo(3);
        Map<String, Object> item = mapper.readValue(summary.get(0).toString(), new TypeReference<>() {
        });
        assertThat(item).containsOnlyKeys("id", "visitedAt", "shop", "menuName", "ramenType", "scores");
        Map<String, Object> shop = mapper.readValue(summary.get(0).get("shop").toString(), new TypeReference<>() {
        });
        assertThat(shop).containsOnlyKeys("id", "name", "branchName");
    }

    @Test
    void 목록은_인증과_크기_및_커서_형식을_검증한다() throws Exception {
        mvc.perform(get("/api/v2/members/me/ramen-logs")).andExpect(status().isUnauthorized());
        for (String size : List.of("0", "51")) {
            mvc.perform(get("/api/v2/members/me/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(owner))
                .param("size", size)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v2/members/me/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(owner))
            .param("cursor", "not-a-cursor")).andExpect(status().isBadRequest());
    }

    @Test
    void 맛_태그_정의는_네_그룹에_각_다섯_개씩_앱_순서대로_제공한다() throws Exception {
        JsonNode definitions = mapper
            .readTree(mvc.perform(get("/api/v2/taste-note-definitions"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray())
            .get("data");
        assertThat(definitions.get("version").asString()).isEqualTo("1");
        JsonNode groups = definitions.get("groups");
        assertThat(groups.size()).isEqualTo(4);
        List<String> categories = List.of("BROTH", "NOODLE", "SEASONING", "TOPPING");
        List<List<String>> labels = List.of(List.of("진해요", "깔끔해요", "감칠맛 좋아요", "기름져요", "어패류 향"),
                List.of("탄력 있어요", "단단해요", "부드러워요", "국물이 잘 배어요", "양 많아요"),
                List.of("딱 좋아요", "슴슴해요", "짭짤해요", "매콤해요", "밥 생각나요"),
                List.of("차슈 좋아요", "계란 좋아요", "멘마 좋아요", "파 향 좋아요", "구성 알차요"));
        for (int index = 0; index < 4; index++) {
            JsonNode group = groups.get(index);
            assertThat(group.get("category").asString()).isEqualTo(categories.get(index));
            assertThat(group.get("notes").size()).isEqualTo(5);
            for (int code = 0; code < 5; code++) {
                assertThat(group.get("notes").get(code).get("code").asString())
                    .isEqualTo(categories.get(index) + "_0" + (code + 1));
                assertThat(group.get("notes").get(code).get("label").asString()).isEqualTo(labels.get(index).get(code));
            }
        }
    }

    private String create(Long userId, String key, String date, String visibility) throws Exception {
        String body = """
                {"shopId":"%d","visitedAt":"%s","menuName":"라멘","ramenType":"쇼유",
                "scores":{"satisfaction":4,"brothDensity":2,"noodleFirmness":3,"topping":4},
                "revisitIntention":"SOMETIMES","note":"방문했어요","tasteNoteCodes":[],
                "visibility":"%s","imageUrls":["https://mock.cdn.com/v2/ramen-logs/photo.png"]}
                """.formatted(SHOP, date, visibility);
        return mapper
            .readTree(mvc.perform(post("/api/v2/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(userId))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsByteArray())
            .get("data")
            .get("id")
            .asString();
    }

    private JsonNode page(String cursor, int size) throws Exception {
        var request = get("/api/v2/members/me/ramen-logs").header(HttpHeaders.AUTHORIZATION, auth(owner))
            .param("size", Integer.toString(size));
        if (cursor != null) {
            request.param("cursor", cursor);
        }
        return mapper
            .readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray())
            .get("data");
    }

    private JsonNode data(String path) throws Exception {
        return mapper
            .readTree(mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, auth(owner)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray())
            .get("data");
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("list-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("L" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        return users.saveAndFlush(user).getId();
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }

}
