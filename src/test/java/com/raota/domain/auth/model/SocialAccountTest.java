package com.raota.domain.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SocialAccountTest {

    @Test
    @DisplayName("프로필 업데이트 시 제공된 이메일로 소셜 계정 이메일을 갱신한다")
    void updateProfile_UpdatesEmailWhenProvided() {
        SocialAccount socialAccount = SocialAccount.builder()
                .provider(AuthProvider.KAKAO)
                .providerUserId("12345")
                .email(null)
                .nickname("old")
                .profileImageUrl("old.jpg")
                .memberId(1L)
                .build();

        socialAccount.updateProfile("kakao@example.com", "new", "new.jpg");

        assertThat(socialAccount.getEmail()).isEqualTo("kakao@example.com");
        assertThat(socialAccount.getNickname()).isEqualTo("new");
        assertThat(socialAccount.getProfileImageUrl()).isEqualTo("new.jpg");
    }

    @Test
    @DisplayName("프로필 업데이트 시 이메일이 없으면 기존 소셜 계정 이메일을 유지한다")
    void updateProfile_KeepsExistingEmailWhenMissing() {
        SocialAccount socialAccount = SocialAccount.builder()
                .provider(AuthProvider.KAKAO)
                .providerUserId("12345")
                .email("kakao@example.com")
                .nickname("old")
                .profileImageUrl("old.jpg")
                .memberId(1L)
                .build();

        socialAccount.updateProfile(null, "new", "new.jpg");

        assertThat(socialAccount.getEmail()).isEqualTo("kakao@example.com");
        assertThat(socialAccount.getNickname()).isEqualTo("new");
        assertThat(socialAccount.getProfileImageUrl()).isEqualTo("new.jpg");
    }
}
