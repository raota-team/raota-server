package com.raota.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.domain.member.model.MemberActivityStats;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.account.infrastructure.auth.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;



import com.raota.support.BaseIntegrationTest;

@Transactional
class MemberInfoAuthIntegrationTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        memberRepository.deleteAll();
    }

    @Test
    void myProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/users/me/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myProfileReturnsAuthenticatedMember() throws Exception {
        MemberProfile member = memberRepository.save(MemberProfile.builder()
                .nickname("테스터")
                .imageUrl("https://example.com/profile.jpg")
                .backgroundImageUrl(null)
                .stats(MemberActivityStats.init())
                .build());

        mockMvc.perform(get("/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createAccessToken(member.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(member.getId()))
                .andExpect(jsonPath("$.data.nickname").value("테스터"));
    }

    @Test
    void updateMyEmail() throws Exception {
        MemberProfile member = memberRepository.save(MemberProfile.builder()
                .nickname("테스터")
                .email("old@example.com")
                .build());

        mockMvc.perform(patch("/users/me/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createAccessToken(member.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("new@example.com"));

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateMyEmailRejectsInvalidEmail() throws Exception {
        MemberProfile member = memberRepository.save(MemberProfile.builder()
                .nickname("테스터")
                .email("old@example.com")
                .build());

        mockMvc.perform(patch("/users/me/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createAccessToken(member.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
