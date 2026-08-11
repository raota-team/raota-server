package com.raota.account.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.application.ramenshop.result.AiRamenShopSearchResult;
import com.raota.agent.application.ramenshop.service.AiRamenShopSearchService;
import com.raota.agent.application.recommendation.RecommendationService;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.model.MemberRole;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.account.infrastructure.auth.JwtTokenProvider;
import com.raota.support.BaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional
class ApiAccessPolicyIntegrationTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private AiRamenShopSearchService aiRamenShopSearchService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void 공개_사용자_경로와_내_경로는_충돌하지_않는다() throws Exception {
        MemberProfile member = saveMember(MemberRole.USER);

        mockMvc.perform(get("/users/{userId}/profile", member.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/me/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/{userId}/ramen-logs", member.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/me/ramen-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void AI와_업로드_티켓은_최소한_인증이_필요하다() throws Exception {
        mockMvc.perform(post("/ramen-shops/ai-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"돈코츠\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/recommendations/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/files/upload-ticket")
                        .queryParam("type", "PROFILE")
                        .queryParam("extension", "jpg"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증된_회원은_보호_경로의_보안_필터를_통과한다() throws Exception {
        MemberProfile member = saveMember(MemberRole.USER);
        String token = jwtTokenProvider.createAccessToken(member.getId());
        when(aiRamenShopSearchService.search("돈코츠", member.getId()))
                .thenReturn(new AiRamenShopSearchResult(List.of()));

        mockMvc.perform(post("/ramen-shops/ai-search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"돈코츠\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 수동_추천_생성은_ADMIN만_접근할_수_있다() throws Exception {
        mockMvc.perform(post("/api/v1/discovery/today-recommendations/generate"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/discovery/today-recommendations/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(createAccessToken(MemberRole.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/v1/discovery/today-recommendations/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(createAccessToken(MemberRole.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void health만_공개하고_나머지_Actuator는_ADMIN으로_제한한다() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(head("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, bearer(createAccessToken(MemberRole.USER))))
                .andExpect(status().isForbidden());

        MvcResult adminResult = mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, bearer(createAccessToken(MemberRole.ADMIN))))
                .andReturn();
        assertThat(adminResult.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void 미분류_경로는_기본적으로_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/security-policy-unclassified"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/security-policy-unclassified")
                        .header(HttpHeaders.AUTHORIZATION, bearer(createAccessToken(MemberRole.USER))))
                .andExpect(status().isNotFound());
    }

    @Test
    void 동적_공개_경로와_같은_위치의_문자열_경로는_기본적으로_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/community/posts/drafts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ramen-shops/internal"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ramen-logs/moderation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 공개_경로라도_잘못된_Bearer_토큰은_401을_반환한다() throws Exception {
        mockMvc.perform(get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    private String createAccessToken(MemberRole role) {
        return jwtTokenProvider.createAccessToken(saveMember(role).getId());
    }

    private MemberProfile saveMember(MemberRole role) {
        return memberRepository.saveAndFlush(MemberProfile.builder()
                .nickname("접근 정책 테스트 회원")
                .role(role)
                .build());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
