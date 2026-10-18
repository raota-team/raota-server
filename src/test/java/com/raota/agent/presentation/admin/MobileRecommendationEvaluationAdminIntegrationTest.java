package com.raota.agent.presentation.admin;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.support.BaseIntegrationTest;
import com.raota.web.account.domain.member.model.MemberProfile;
import com.raota.web.account.domain.member.model.MemberRole;
import com.raota.web.account.domain.member.repository.MemberRepository;
import com.raota.web.account.infrastructure.auth.JwtTokenProvider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 일회용 위치 필터 평가 API와 함께 지운다. */
class MobileRecommendationEvaluationAdminIntegrationTest extends BaseIntegrationTest {

    private static final String PATH = "/admin/api/v2/recommendation-evaluations/location";

    private static final long MAPO = 900005001L;

    private static final long GANGNAM = 900005002L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberRepository members;

    @Autowired
    private JwtTokenProvider webTokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        redis.delete(List.of("v2:recommendation-eval:location:latest", "lock:v2:recommendation-eval:location"));
        insertShop(MAPO, "마포구", "서울 마포구 망원로 1");
        insertShop(GANGNAM, "강남구", "서울 강남구 테헤란로 1");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(document(GANGNAM, 0.9), document(MAPO, 0.8)));
    }

    @AfterEach
    void cleanUp() {
        redis.delete(List.of("v2:recommendation-eval:location:latest", "lock:v2:recommendation-eval:location"));
        jdbc.update("DELETE FROM tb_v2_shop WHERE id IN (?, ?)", MAPO, GANGNAM);
    }

    @Test
    void DEV_검색_사례를_지역_없이와_질문의_지역으로_비교해_결과를_남긴다() throws Exception {
        String admin = "Bearer " + webToken(MemberRole.ADMIN);
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist());

        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isAccepted());
        await().atMost(Duration.ofSeconds(20))
            .until(() -> Boolean.TRUE.equals(redis.hasKey("v2:recommendation-eval:location:latest")));

        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.datasetVersion").value("rag-mobile-v1.1"))
            .andExpect(jsonPath("$.data.cases.length()").value(8))
            .andExpect(jsonPath("$.data.cases[?(@.query == '망원 꼬들한 면 시오라멘')].region").value("서울 마포구"))
            .andExpect(jsonPath("$.data.cases[?(@.query == '망원 꼬들한 면 시오라멘')].baseShopIds[0]")
                .value(Math.toIntExact(GANGNAM)))
            .andExpect(jsonPath("$.data.cases[?(@.query == '망원 꼬들한 면 시오라멘')].locatedShopIds[0]")
                .value(Math.toIntExact(MAPO)))
            .andExpect(jsonPath("$.data.cases[?(@.query == '미소 라멘 깊은 감칠맛')].locatedShopIds[0]")
                .value(Math.toIntExact(GANGNAM)))
            .andExpect(jsonPath("$.data.cases[?(@.error != null)]").isEmpty());
    }

    @Test
    void 웹_관리자만_실행할_수_있다() throws Exception {
        mvc.perform(post(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + webToken(MemberRole.USER)))
            .andExpect(status().isForbidden());
    }

    private static Document document(long shopId, double score) {
        return Document.builder()
            .text("매장 " + shopId)
            .metadata(Map.of("documentType", "SHOP_PROFILE", "shopId", String.valueOf(shopId)))
            .score(score)
            .build();
    }

    private String webToken(MemberRole role) {
        MemberProfile member = members.saveAndFlush(MemberProfile.builder().nickname("평가 관리자").role(role).build());
        return webTokens.createAccessToken(member.getId());
    }

    private void insertShop(long id, String region, String address) {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, is_published, created_at, updated_at)
                VALUES (?, '평가 매장', '본점', ?, ?, '[]', '[]', 'OPERATIONAL', '[]', TRUE, '2026-09-01 00:00:00',
                    '2026-09-01 00:00:00')
                """, id, address, region);
    }

}
