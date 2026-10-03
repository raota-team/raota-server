package com.raota.mobile.ramenlog.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileUploadTicketIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService tokens;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private Long userId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        userId = users.saveAndFlush(MobileUser.onboarding("upload-test@example.com")).getId();
    }

    @AfterEach
    void cleanUp() {
        users.deleteById(userId);
    }

    @Test
    void 온보딩_회원에게_라멘과_프로필_사진의_PUT_티켓을_발급한다() throws Exception {
        JsonNode ramen = ticket("""
                {"purpose":"RAMEN_LOG","files":[{"contentType":"image/jpeg","extension":"jpg"},
                {"contentType":"image/webp","extension":"webp"}]}
                """);
        assertThat(ramen.size()).isEqualTo(2);
        assertThat(ramen.get(0).get("method").asString()).isEqualTo("PUT");
        assertThat(ramen.get(0).get("url").asString()).isEqualTo("/files/mock-upload-endpoint");
        assertThat(ramen.get(0).get("fields").isNull()).isTrue();
        assertThat(ramen.get(0).get("headers").get("Content-Type").asString()).isEqualTo("image/jpeg");
        assertThat(ramen.get(0).get("imageUrl").asString()).startsWith("https://mock.cdn.com/v2/ramen-logs/")
            .endsWith(".jpg");
        assertThat(ramen.get(1).get("imageUrl").asString()).endsWith(".webp");

        JsonNode profile = ticket("""
                {"purpose":"PROFILE","files":[{"contentType":"image/png","extension":"png"}]}
                """);
        assertThat(profile.get(0).get("imageUrl").asString()).startsWith("https://mock.cdn.com/v2/profiles/")
            .endsWith(".png");
    }

    @Test
    void 장수와_사진_형식이_올바르지_않으면_400이다() throws Exception {
        for (String request : new String[] { """
                {"purpose":"RAMEN_LOG","files":[{"contentType":"image/png","extension":"png"},
                {"contentType":"image/png","extension":"png"},{"contentType":"image/png","extension":"png"},
                {"contentType":"image/png","extension":"png"}]}
                """, """
                {"purpose":"PROFILE","files":[{"contentType":"image/png","extension":"png"},
                {"contentType":"image/png","extension":"png"}]}
                """, """
                {"purpose":"RAMEN_LOG","files":[{"contentType":"application/pdf","extension":"png"}]}
                """, """
                {"purpose":"RAMEN_LOG","files":[{"contentType":"image/png","extension":"jpg"}]}
                """ }) {
            mvc.perform(post("/api/v2/files/upload-tickets").header(HttpHeaders.AUTHORIZATION, authorization())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void 로그인하지_않으면_발급하지_않는다() throws Exception {
        mvc.perform(post("/api/v2/files/upload-tickets").contentType(MediaType.APPLICATION_JSON).content("""
                {"purpose":"PROFILE","files":[{"contentType":"image/png","extension":"png"}]}
                """)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private JsonNode ticket(String request) throws Exception {
        return mapper
            .readTree(
                    mvc.perform(post("/api/v2/files/upload-tickets").header(HttpHeaders.AUTHORIZATION, authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray())
            .get("data")
            .get("uploads");
    }

    private String authorization() {
        return "Bearer " + tokens.issue(userId);
    }

}
