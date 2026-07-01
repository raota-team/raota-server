package com.raota.presentation.admin.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.raota.domain.auth.model.AuthProvider;
import com.raota.domain.auth.model.SocialAccount;
import com.raota.domain.auth.repository.SocialAccountRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.support.BaseIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional
class AdminUserControllerTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        socialAccountRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void adminHomeRenders() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("라멘집 관리")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("유저 관리")));
    }

    @Test
    void userListRendersAndFilters() throws Exception {
        MemberProfile kakaoMember = saveMember("카카오회원", "kakao@example.com", true, false);
        saveSocialAccount(kakaoMember, AuthProvider.KAKAO, "kakao-1", "kakao@example.com");
        MemberProfile googleMember = saveMember("구글회원", null, false, false);
        saveSocialAccount(googleMember, AuthProvider.GOOGLE, "google-1", null);
        MemberProfile deletedMember = saveMember("탈퇴회원", "deleted@example.com", true, true);
        saveSocialAccount(deletedMember, AuthProvider.KAKAO, "kakao-2", "deleted@example.com");

        assertBody(getBody("/admin/users?keyword=카카오"))
                .contains("카카오회원")
                .doesNotContain("구글회원");
        assertBody(getBody("/admin/users?keyword=" + kakaoMember.getId()))
                .contains("카카오회원")
                .doesNotContain("구글회원");
        assertBody(getBody("/admin/users?provider=GOOGLE"))
                .contains("구글회원")
                .doesNotContain("카카오회원");
        assertBody(getBody("/admin/users?registrationCompleted=false"))
                .contains("구글회원")
                .doesNotContain("카카오회원");
        assertBody(getBody("/admin/users?deleted=true"))
                .contains("탈퇴회원")
                .doesNotContain("카카오회원");
        assertBody(getBody("/admin/users?emailPresent=false"))
                .contains("구글회원")
                .doesNotContain("kakao@example.com");
    }

    @Test
    void userDetailRendersProfileSocialStatsAndVisibility() throws Exception {
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

        mockMvc.perform(get("/admin/users/{memberId}", member.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("상세회원")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("detail@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("social-detail@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("라멘 좋아함")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("kakao-detail")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">7<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("비공개")));
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
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private org.assertj.core.api.AbstractStringAssert<?> assertBody(String body) {
        assertThat(body).isNotBlank();
        return assertThat(body);
    }
}
