package com.raota.web.account.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.support.BaseIntegrationTest;
import com.raota.web.account.domain.member.model.MemberProfile;
import com.raota.web.account.domain.member.model.MemberRole;
import com.raota.web.account.domain.member.repository.MemberRepository;
import com.raota.web.account.infrastructure.auth.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Transactional
class MobileApiFallbackIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
    void 인증된_사용자의_없는_v2_경로는_v2_404_응답을_반환한다() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v2/no-such-path").header(HttpHeaders.AUTHORIZATION, bearerToken()))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.get("meta").get("requestId").asString())
            .isEqualTo(result.getResponse().getHeader(RequestIdFilter.HEADER));
    }

    @Test
    void 인증된_사용자의_없는_v1_경로는_본문_없는_404를_유지한다() throws Exception {
        mockMvc.perform(get("/no-such-v1-path").header(HttpHeaders.AUTHORIZATION, bearerToken()))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));
    }

    @Test
    void 인증되지_않은_사용자의_없는_v2_경로는_기존_401_응답을_유지한다() throws Exception {
        mockMvc.perform(get("/api/v2/no-such-path"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private String bearerToken() {
        MemberProfile member = memberRepository
            .saveAndFlush(MemberProfile.builder().nickname("v2 오류 응답 테스트 회원").role(MemberRole.USER).build());
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }

}
