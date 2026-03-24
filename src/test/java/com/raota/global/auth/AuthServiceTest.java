package com.raota.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.raota.domain.auth.model.AuthProvider;
import com.raota.domain.auth.model.SocialAccount;
import com.raota.domain.auth.service.AuthAccountService;
import com.raota.domain.auth.service.AuthService;
import com.raota.domain.auth.service.OAuth2LoginResult;
import com.raota.domain.auth.service.OAuth2UserInfo;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.service.MemberProvisioningService;
import com.raota.global.auth.AuthenticatedMember;
import com.raota.global.auth.AuthProperties;
import com.raota.global.auth.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private MemberProvisioningService memberProvisioningService;
    @Mock
    private AuthAccountService authAccountService;

    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    private OAuth2UserInfo info;
    private MemberProfile memberProfile;

    @BeforeEach
    void setup(){
        AuthProperties properties = new AuthProperties(
                "test-issuer",
                "test-secret-key-at-least-32-characters-long-for-hs256",
                3600L,
                86400L,
                new AuthProperties.OAuth2("http://localhost:3000/callback", "http://localhost:3000/error"),
                new AuthProperties.Cookie("refreshToken", true, "None", "localhost"),
                new AuthProperties.Cors(java.util.List.of("http://localhost:3000"))
        );

        jwtTokenProvider = new JwtTokenProvider(properties);
        authService = new AuthService(memberProvisioningService, authAccountService, jwtTokenProvider);

        info = new OAuth2UserInfo(
                AuthProvider.GOOGLE,
                "test",
                "test@gmail.com",
                "테스트",
                "test.jpg"
        );
        memberProfile = MemberProfile.builder()
                .id(1L)
                .nickname("test-user")
                .imageUrl("http://example.com/image.png")
                .build();
    }

    @Test
    @DisplayName("신규 회원가입")
    void signUp_NewMember(){
        // given
        given(authAccountService.findSocialAccount(any())).willReturn(Optional.empty());
        given(memberProvisioningService.createOAuthMember(any(), any())).willReturn(memberProfile);
        given(authAccountService.login(any(), any(), eq(memberProfile.getId()))).willReturn("test-refresh-token");

        // when
        OAuth2LoginResult result = authService.login(info);

        // then
        assertThat(result.newMember()).isTrue();
        assertThat(result.memberId()).isEqualTo(memberProfile.getId());
        assertThat(result.refreshToken()).isEqualTo("test-refresh-token");

        // 실제 토큰 유효성 검증
        assertThat(result.accessToken()).isNotBlank();
        AuthenticatedMember authenticatedMember = jwtTokenProvider.getAuthenticatedMember(result.accessToken());
        assertThat(authenticatedMember.memberId()).isEqualTo(memberProfile.getId());

        verify(memberProvisioningService).createOAuthMember(any(), any());
        verify(authAccountService).login(any(), any(), eq(memberProfile.getId()));
    }

    @Test
    @DisplayName("이미 있는 회원")
    void login_ExistingMember(){
        // given
        SocialAccount existingAccount = SocialAccount.builder()
                        .provider(info.provider())
                        .providerUserId(info.providerUserId())
                        .email(info.email())
                        .nickname(info.nickname())
                        .profileImageUrl(info.profileImageUrl())
                        .memberId(memberProfile.getId())
                        .build();

        given(authAccountService.findSocialAccount(any())).willReturn(Optional.of(existingAccount));
        given(memberProvisioningService.getRequired(existingAccount.getMemberId())).willReturn(memberProfile);
        given(authAccountService.login(any(), any(), eq(memberProfile.getId()))).willReturn("test-refresh-token");

        // when
        OAuth2LoginResult result = authService.login(info);

        // then
        assertThat(result.newMember()).isFalse();
        assertThat(result.memberId()).isEqualTo(memberProfile.getId());
        assertThat(result.refreshToken()).isEqualTo("test-refresh-token");

        // 실제 토큰 유효성 검증
        AuthenticatedMember authenticatedMember = jwtTokenProvider.getAuthenticatedMember(result.accessToken());
        assertThat(authenticatedMember.memberId()).isEqualTo(memberProfile.getId());

        verify(memberProvisioningService, never()).createOAuthMember(any(), any());
        verify(authAccountService).login(any(), any(), eq(memberProfile.getId()));
    }
}
