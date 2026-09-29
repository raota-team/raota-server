package com.raota.web.account.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.mobile.account.infrastructure.auth.MobileAuthProperties;
import com.raota.web.account.infrastructure.auth.AuthProperties;
import com.raota.web.account.infrastructure.auth.JwtTokenProvider;
import com.raota.support.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Transactional
class MobileApiSecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MobileUserRepository mobileUsers;

    @Autowired
    private MobileAccessTokenService mobileAccessTokens;

    @Autowired
    private MobileAuthProperties mobileAuthProperties;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(requestIdFilter)
            .apply(springSecurity())
            .build();
    }

    @Test
    void v2_경로에_잘못된_bearer_토큰을_보내면_401_v2_응답을_반환한다() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/api/v2/unclassified-probe").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.error.fields").isEmpty())
            .andExpect(jsonPath("$.status").doesNotExist())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.get("data").isNull()).isTrue();
        assertRequestIdMatchesHeader(result.getResponse(), body);
    }

    @Test
    void 만료된_v2_액세스_토큰은_TOKEN_EXPIRED를_반환한다() throws Exception {
        String expiredToken = new MobileAccessTokenService(
                new MobileAuthProperties(mobileAuthProperties.issuer(), mobileAuthProperties.accessTokenSecret(), -60))
            .issue(1L);
        MvcResult result = mockMvc
            .perform(get("/api/v2/unclassified-probe").header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"))
            .andExpect(jsonPath("$.error.message").value("액세스 토큰이 만료되었습니다."))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertRequestIdMatchesHeader(result.getResponse(), body);
    }

    @Test
    void 분류되지_않은_v2_경로는_v2_401_응답을_반환한다() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v2/unclassified-probe"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertRequestIdMatchesHeader(result.getResponse(), body);
    }

    @Test
    void v1_만료된_액세스_토큰은_기존_401_응답을_유지한다() throws Exception {
        String expiredToken = expiredTokenProvider().createAccessToken(1L);

        mockMvc.perform(get("/").header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("FAIL"))
            .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void v1_토큰은_v2_요청에서_인증할_수_없다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/v2/unclassified-probe").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void 인코딩된_v2_경로에서도_v1_토큰으로_인증할_수_없다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L);

        mockMvc
            .perform(get(URI.create("/%61pi/v2/unclassified-probe")).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void v2_토큰은_v1_인증_경로에서_인증할_수_없다() throws Exception {
        String token = mobileAccessTokens.issue(saveMobileUser());

        mockMvc.perform(get("/users/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("FAIL"))
            .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(value = MobileUserStatus.class, names = { "SUSPENDED", "WITHDRAW_PENDING", "WITHDRAWN" })
    void 사용할_수_없는_모바일_회원은_인증을_거부한다(MobileUserStatus status) throws Exception {
        Long userId = saveMobileUser();
        jdbcTemplate.update("UPDATE tb_v2_user SET status = ? WHERE id = ?", status.name(), userId);
        entityManager.clear();

        mockMvc
            .perform(get("/api/v2/unclassified-probe").header(HttpHeaders.AUTHORIZATION,
                    bearer(mobileAccessTokens.issue(userId))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.error.message").value("사용할 수 없는 계정입니다."));
    }

    @Test
    void 존재하지_않는_모바일_회원은_인증을_거부한다() throws Exception {
        mockMvc
            .perform(get("/api/v2/unclassified-probe").header(HttpHeaders.AUTHORIZATION,
                    bearer(mobileAccessTokens.issue(Long.MAX_VALUE))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.error.message").value("사용할 수 없는 계정입니다."));
    }

    @Test
    void 활성_모바일_회원의_토큰은_없는_경로에서_404를_반환한다() throws Exception {
        Long userId = saveMobileUser();
        jdbcTemplate.update("UPDATE tb_v2_user SET status = 'ACTIVE' WHERE id = ?", userId);
        entityManager.clear();

        mockMvc
            .perform(get("/api/v2/unclassified-probe").header(HttpHeaders.AUTHORIZATION,
                    bearer(mobileAccessTokens.issue(userId))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    private Long saveMobileUser() {
        return mobileUsers.saveAndFlush(MobileUser.onboarding("security-test@example.com")).getId();
    }

    private JwtTokenProvider expiredTokenProvider() {
        return new JwtTokenProvider(new AuthProperties(authProperties.issuer(), authProperties.accessTokenSecret(), -60,
                authProperties.refreshTokenExpirySeconds(), authProperties.oauth2(), authProperties.cookie(),
                authProperties.cors()));
    }

    private void assertRequestIdMatchesHeader(MockHttpServletResponse response, JsonNode body) {
        assertThat(body.get("meta").get("requestId").asString()).isEqualTo(response.getHeader(RequestIdFilter.HEADER));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

}
