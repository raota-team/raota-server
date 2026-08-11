package com.raota.integration.infrastructure.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.model.MemberRole;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.account.infrastructure.auth.JwtTokenProvider;
import com.raota.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional
class AdminAccessPolicyIntegrationTest extends BaseIntegrationTest {

    private static final String ADMIN_RESOURCE = "/admin/api/ramen-shops";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void 익명은_관리자_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get(ADMIN_RESOURCE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAIL"));
    }

    @Test
    void USER는_관리자_API에_접근할_수_없다() throws Exception {
        String token = createAccessToken(MemberRole.USER);

        mockMvc.perform(get(ADMIN_RESOURCE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FAIL"));
    }

    @Test
    void ADMIN은_관리자_API에_접근할_수_있다() throws Exception {
        String token = createAccessToken(MemberRole.ADMIN);

        mockMvc.perform(get(ADMIN_RESOURCE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void 같은_토큰도_DB의_현재_역할을_즉시_반영한다() throws Exception {
        MemberProfile member = saveMember(MemberRole.USER);
        String token = jwtTokenProvider.createAccessToken(member.getId());

        assertAdminStatus(token, 403);

        updateRole(member.getId(), MemberRole.ADMIN);
        assertAdminStatus(token, 200);

        updateRole(member.getId(), MemberRole.USER);
        assertAdminStatus(token, 403);
    }

    private String createAccessToken(MemberRole role) {
        MemberProfile member = saveMember(role);
        return jwtTokenProvider.createAccessToken(member.getId());
    }

    private MemberProfile saveMember(MemberRole role) {
        return memberRepository.saveAndFlush(MemberProfile.builder()
                .nickname("보안 정책 테스트 회원")
                .role(role)
                .build());
    }

    private void updateRole(Long memberId, MemberRole role) {
        jdbcTemplate.update(
                "UPDATE tb_member_profile SET role = ? WHERE id = ?",
                role.name(),
                memberId
        );
    }

    private void assertAdminStatus(String token, int expectedStatus) throws Exception {
        mockMvc.perform(get(ADMIN_RESOURCE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().is(expectedStatus));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
