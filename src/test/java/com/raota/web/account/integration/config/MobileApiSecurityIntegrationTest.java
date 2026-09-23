package com.raota.web.account.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.web.account.infrastructure.auth.AuthProperties;
import com.raota.web.account.infrastructure.auth.JwtTokenProvider;
import com.raota.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileApiSecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private ObjectMapper objectMapper;

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
        MvcResult result = mockMvc.perform(get("/api/v2/unclassified-probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
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
        String expiredToken = expiredTokenProvider().createAccessToken(1L);
        MvcResult result = mockMvc.perform(get("/api/v2/unclassified-probe")
                        .header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
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

        mockMvc.perform(get("/")
                        .header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    private JwtTokenProvider expiredTokenProvider() {
        return new JwtTokenProvider(new AuthProperties(
                authProperties.issuer(),
                authProperties.accessTokenSecret(),
                -60,
                authProperties.refreshTokenExpirySeconds(),
                authProperties.oauth2(),
                authProperties.cookie(),
                authProperties.cors()
        ));
    }

    private void assertRequestIdMatchesHeader(MockHttpServletResponse response, JsonNode body) {
        assertThat(body.get("meta").get("requestId").asString())
                .isEqualTo(response.getHeader(RequestIdFilter.HEADER));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
