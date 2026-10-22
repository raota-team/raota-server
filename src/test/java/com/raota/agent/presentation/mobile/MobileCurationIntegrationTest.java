package com.raota.agent.presentation.mobile;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class MobileCurationIntegrationTest extends BaseIntegrationTest {

    private static final long RAMEN_TYPE = 900004701L;

    private static final long POPULAR = 900004711L;

    private static final long LESS_POPULAR = 900004712L;

    private static final long HIDDEN = 900004713L;

    private static final long CLOSED = 900004714L;

    private static final long TAGGED = 900004715L;

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
    private StringRedisTemplate redis;

    private MockMvc mvc;

    private Long member;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        redis.delete("curation:daily:latest");
        insertShop(POPULAR, "[\"돈코츠\"]", "[]", 9, "OPERATIONAL", true);
        insertShop(LESS_POPULAR, "[\"돈코츠\", \"쇼유\"]", "[]", 5, "OPERATIONAL", true);
        insertShop(HIDDEN, "[\"돈코츠\"]", "[]", 99, "UNKNOWN", false);
        insertShop(CLOSED, "[\"돈코츠\"]", "[]", 50, "CLOSED", true);
        insertShop(TAGGED, "[\"기타\"]", "[\"차슈 멘\", \"닭\"]", 1, "OPERATIONAL", true);
        MobileUser user = MobileUser.onboarding("curation-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("C" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        member = users.saveAndFlush(user).getId();
        jdbc.update(
                "INSERT INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))",
                member, POPULAR);
    }

    @AfterEach
    void cleanUp() {
        redis.delete("curation:daily:latest");
        jdbc.update("DELETE FROM tb_daily_curation WHERE ramen_type_id = ?", RAMEN_TYPE);
        jdbc.update("DELETE FROM tb_ramen_type WHERE id = ?", RAMEN_TYPE);
        jdbc.update("DELETE FROM tb_v2_shop_bookmark WHERE user_id = ?", member);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id BETWEEN ? AND ?", POPULAR, TAGGED);
        users.deleteById(member);
    }

    @Test
    void 날씨로_고른_종류의_추천해도_되는_매장_중_조회수_1위를_붙인다() throws Exception {
        insertCuration("돈코츠 라멘 ");

        mvc.perform(get("/api/v2/curations/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.shop.id").value(String.valueOf(POPULAR)))
            .andExpect(jsonPath("$.data.shop.isBookmarked").value(false))
            .andExpect(jsonPath("$.data.title").value("비 오는 날의 돈코츠"))
            .andExpect(jsonPath("$.data.reason").value("진한 국물이 어울립니다."))
            .andExpect(jsonPath("$.meta.requestId").isNotEmpty());
        mvc.perform(get("/api/v2/curations/today").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(member)))
            .andExpect(jsonPath("$.data.shop.isBookmarked").value(true));
    }

    @Test
    void 앱_종류에_없는_종류는_매장_태그로_찾는다() throws Exception {
        insertCuration("차슈멘");

        mvc.perform(get("/api/v2/curations/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.shop.id").value(String.valueOf(TAGGED)));
    }

    @Test
    void 맞는_매장이_없으면_data는_null이다() throws Exception {
        insertCuration("중화소바");

        mvc.perform(get("/api/v2/curations/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    private void insertCuration(String ramenTypeName) {
        jdbc.update("INSERT INTO tb_ramen_type (id, name, sub_title, image_url) VALUES (?, ?, '', '')", RAMEN_TYPE,
                ramenTypeName);
        jdbc.update("""
                INSERT INTO tb_daily_curation (date_key, ramen_type_id, title, reason, created_at)
                VALUES (99991231, ?, '비 오는 날의 돈코츠', '진한 국물이 어울립니다.', CURRENT_TIMESTAMP)
                """, RAMEN_TYPE);
    }

    private void insertShop(long id, String ramenTypes, String tags, int views, String status, boolean published) {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, view_count, is_published, created_at, updated_at)
                VALUES (?, '큐레이션 매장', '본점', '서울', '서울', ?, ?, ?, '[]', ?, ?, '2026-09-01 00:00:00',
                    '2026-09-01 00:00:00')
                """, id, ramenTypes, tags, status, views, published);
    }

}
