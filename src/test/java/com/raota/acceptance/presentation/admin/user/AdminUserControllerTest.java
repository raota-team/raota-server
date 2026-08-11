package com.raota.acceptance.presentation.admin.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.account.domain.auth.model.AuthProvider;
import com.raota.account.domain.auth.model.SocialAccount;
import com.raota.account.domain.auth.repository.SocialAccountRepository;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.model.MemberRole;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.account.infrastructure.auth.JwtTokenProvider;
import com.raota.support.BaseIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional
class AdminUserControllerTest extends BaseIntegrationTest {

    private MockMvc mockMvc;
    private String adminToken;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        socialAccountRepository.deleteAll();
        memberRepository.deleteAll();

        MemberProfile admin = memberRepository.saveAndFlush(MemberProfile.builder()
                .nickname("관리자")
                .role(MemberRole.ADMIN)
                .build());
        adminToken = jwtTokenProvider.createAccessToken(admin.getId());
    }

    @Test
    void 사용자_목록을_JSON으로_조회하고_필터링한다() throws Exception {
        MemberProfile kakaoMember = saveMember("카카오회원", "kakao@example.com", true, false);
        saveSocialAccount(kakaoMember, AuthProvider.KAKAO, "kakao-1", "kakao@example.com");
        MemberProfile googleMember = saveMember("구글회원", null, false, false);
        saveSocialAccount(googleMember, AuthProvider.GOOGLE, "google-1", null);
        MemberProfile deletedMember = saveMember("탈퇴회원", "deleted@example.com", true, true);
        saveSocialAccount(deletedMember, AuthProvider.KAKAO, "kakao-2", "deleted@example.com");

        mockMvc.perform(adminGet("/admin/api/users")
                        .param("keyword", "카카오"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].id").value(kakaoMember.getId()))
                .andExpect(jsonPath("$.data.items[0].role").value("USER"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));

        assertBody(getBody("/admin/api/users?keyword=" + kakaoMember.getId()))
                .contains("카카오회원")
                .doesNotContain("구글회원");
        assertBody(getBody("/admin/api/users?provider=GOOGLE"))
                .contains("구글회원")
                .doesNotContain("카카오회원");
        assertBody(getBody("/admin/api/users?registrationCompleted=false"))
                .contains("구글회원")
                .doesNotContain("카카오회원");
        assertBody(getBody("/admin/api/users?deleted=true"))
                .contains("탈퇴회원")
                .doesNotContain("카카오회원");
        assertBody(getBody("/admin/api/users?emailPresent=false"))
                .contains("구글회원")
                .doesNotContain("kakao@example.com");
    }

    @Test
    void 사용자_상세를_JSON으로_조회한다() throws Exception {
        MemberProfile member = MemberProfile.builder()
                .nickname("상세회원")
                .email("detail@example.com")
                .imageUrl("profile/detail.jpg")
                .backgroundImageUrl("background/detail.jpg")
                .bio("라멘 좋아함")
                .build();
        member.completeRegistration();
        member.updateActivityVisibility(true, false, true, false);
        repeat(3, member::increaseVisitedRestaurantCount);
        repeat(4, member::increasePhotoCount);
        repeat(5, member::increaseBookmarkCount);
        repeat(6, member::increasePostCount);
        repeat(7, member::increaseCommentCount);
        member = memberRepository.save(member);
        saveSocialAccount(member, AuthProvider.KAKAO, "kakao-detail", "social-detail@example.com");

        mockMvc.perform(adminGet("/admin/api/users/{memberId}", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(member.getId()))
                .andExpect(jsonPath("$.data.profile.nickname").value("상세회원"))
                .andExpect(jsonPath("$.data.profile.email").value("detail@example.com"))
                .andExpect(jsonPath("$.data.profile.role").value("USER"))
                .andExpect(jsonPath("$.data.socialAccounts[0].email").value("social-detail@example.com"))
                .andExpect(jsonPath("$.data.socialAccounts[0].providerUserId").value("kakao-detail"))
                .andExpect(jsonPath("$.data.activityStats.commentCount").value(7))
                .andExpect(jsonPath("$.data.activityVisibility.visitsPublic").value(false));
    }

    private MemberProfile saveMember(String nickname, String email, boolean registrationCompleted, boolean deleted) {
        MemberProfile member = MemberProfile.builder()
                .nickname(nickname)
                .email(email)
                .build();
        if (registrationCompleted) {
            member.completeRegistration();
        }
        if (deleted) {
            member.softDelete(LocalDateTime.now());
        }
        return memberRepository.save(member);
    }

    private void saveSocialAccount(MemberProfile member, AuthProvider provider, String providerUserId, String email) {
        socialAccountRepository.save(SocialAccount.builder()
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .nickname(member.getNickname())
                .profileImageUrl(member.getImageUrl())
                .memberId(member.getId())
                .build());
    }

    private void repeat(int times, Runnable runnable) {
        for (int index = 0; index < times; index++) {
            runnable.run();
        }
    }

    private String getBody(String path) throws Exception {
        MvcResult result = mockMvc.perform(adminGet(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private MockHttpServletRequestBuilder adminGet(String path, Object... uriVariables) {
        return get(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken);
    }

    private org.assertj.core.api.AbstractStringAssert<?> assertBody(String body) {
        assertThat(body).isNotBlank();
        return assertThat(body);
    }
}
