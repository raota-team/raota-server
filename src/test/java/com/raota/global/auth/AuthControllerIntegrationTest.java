package com.raota.global.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.domain.auth.model.RefreshToken;
import com.raota.domain.auth.repository.RefreshTokenRepository;
import com.raota.domain.member.model.MemberActivityStats;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import java.time.Instant;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void refreshIssuesNewAccessTokenAndRotatesCookie() throws Exception {
        MemberProfile member = memberRepository.save(MemberProfile.builder()
                .nickname("리프레시유저")
                .imageUrl(null)
                .backgroundImageUrl(null)
                .stats(MemberActivityStats.init())
                .build());
        refreshTokenRepository.save(RefreshToken.builder()
                .memberId(member.getId())
                .token("refresh-token-value")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("raota_refresh_token", "refresh-token-value")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.memberId").value(member.getId()))
                .andExpect(header().string("Set-Cookie", containsString("raota_refresh_token=")));
    }
}
