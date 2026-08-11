package com.raota.account.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.account.domain.auth.model.AuthProvider;
import com.raota.account.domain.auth.model.SocialAccount;
import com.raota.account.application.auth.AuthAccountService;
import com.raota.account.application.auth.AuthService;
import com.raota.account.application.auth.OAuth2LoginResult;
import com.raota.account.application.auth.OAuth2UserInfo;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.application.member.MemberProvisioningService;
import com.raota.account.infrastructure.auth.AuthProperties;
import com.raota.account.infrastructure.auth.JwtTokenProvider;
import com.raota.account.infrastructure.auth.WithdrawnMemberException;
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
        assertThat(memberProfile.getEmail()).isEqualTo(info.email());

        // 실제 토큰 유효성 검증
        assertThat(result.accessToken()).isNotBlank();
        assertThat(jwtTokenProvider.getMemberId(result.accessToken())).isEqualTo(memberProfile.getId());

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
        memberProfile.completeRegistration(); // 가입 완료 상태로 변경
        given(memberProvisioningService.findById(existingAccount.getMemberId())).willReturn(Optional.of(memberProfile));
        given(authAccountService.login(any(), any(), eq(memberProfile.getId()))).willReturn("test-refresh-token");

        // when
        OAuth2LoginResult result = authService.login(info);

        // then
        assertThat(result.newMember()).isFalse();
        assertThat(result.memberId()).isEqualTo(memberProfile.getId());
        assertThat(result.refreshToken()).isEqualTo("test-refresh-token");
        assertThat(memberProfile.getEmail()).isEqualTo(info.email());

        // 실제 토큰 유효성 검증
        assertThat(jwtTokenProvider.getMemberId(result.accessToken())).isEqualTo(memberProfile.getId());

        verify(memberProvisioningService, never()).createOAuthMember(any(), any());
        verify(authAccountService).login(any(), any(), eq(memberProfile.getId()));
    }

    @Test
    @DisplayName("이미 대표 이메일이 있는 회원은 재로그인 시 소셜 이메일로 덮어쓰지 않는다")
    void login_ExistingMemberKeepsMemberEmail(){
        OAuth2UserInfo changedSocialEmailInfo = new OAuth2UserInfo(
                AuthProvider.GOOGLE,
                "test",
                "changed@gmail.com",
                "테스트",
                "test.jpg"
        );
        MemberProfile emailOwnedMemberProfile = MemberProfile.builder()
                .id(1L)
                .nickname("test-user")
                .email("member@gmail.com")
                .imageUrl("http://example.com/image.png")
                .build();
        emailOwnedMemberProfile.completeRegistration();
        SocialAccount existingAccount = SocialAccount.builder()
                .provider(changedSocialEmailInfo.provider())
                .providerUserId(changedSocialEmailInfo.providerUserId())
                .email("old-social@gmail.com")
                .nickname(changedSocialEmailInfo.nickname())
                .profileImageUrl(changedSocialEmailInfo.profileImageUrl())
                .memberId(emailOwnedMemberProfile.getId())
                .build();

        given(authAccountService.findSocialAccount(any())).willReturn(Optional.of(existingAccount));
        given(memberProvisioningService.findById(existingAccount.getMemberId())).willReturn(Optional.of(emailOwnedMemberProfile));
        given(authAccountService.login(any(), any(), eq(emailOwnedMemberProfile.getId()))).willReturn("test-refresh-token");

        authService.login(changedSocialEmailInfo);

        assertThat(emailOwnedMemberProfile.getEmail()).isEqualTo("member@gmail.com");
        verify(authAccountService).login(any(), any(), eq(emailOwnedMemberProfile.getId()));
    }

    @Test
    @DisplayName("소셜 계정은 있지만 회원 프로필이 없는 경우 (데이터 불일치 복구)")
    void login_ExistingSocialAccountButMissingMemberProfile(){
        // given
        SocialAccount existingAccount = SocialAccount.builder()
                        .provider(info.provider())
                        .providerUserId(info.providerUserId())
                        .email(info.email())
                        .nickname(info.nickname())
                        .profileImageUrl(info.profileImageUrl())
                        .memberId(999L) // 존재하지 않는 회원 ID
                        .build();

        given(authAccountService.findSocialAccount(any())).willReturn(Optional.of(existingAccount));
        given(memberProvisioningService.findById(existingAccount.getMemberId())).willReturn(Optional.empty()); // 프로필 없음
        given(memberProvisioningService.createOAuthMember(any(), any())).willReturn(memberProfile); // 새 프로필 생성
        given(authAccountService.login(any(), any(), eq(memberProfile.getId()))).willReturn("test-refresh-token");

        // when
        OAuth2LoginResult result = authService.login(info);

        // then
        assertThat(result.newMember()).isTrue();
        assertThat(result.memberId()).isEqualTo(memberProfile.getId());
        assertThat(result.refreshToken()).isEqualTo("test-refresh-token");

        verify(memberProvisioningService).createOAuthMember(any(), any());
        verify(authAccountService).login(any(), any(), eq(memberProfile.getId()));
    }

    @Test
    @DisplayName("탈퇴한 회원은 재로그인할 수 없다")
    void login_WithdrawnMember() {
        SocialAccount existingAccount = SocialAccount.builder()
                .provider(info.provider())
                .providerUserId(info.providerUserId())
                .email(info.email())
                .nickname(info.nickname())
                .profileImageUrl(info.profileImageUrl())
                .memberId(memberProfile.getId())
                .build();

        memberProfile.softDelete(java.time.LocalDateTime.now());

        given(authAccountService.findSocialAccount(any())).willReturn(Optional.of(existingAccount));
        given(memberProvisioningService.findById(existingAccount.getMemberId())).willReturn(Optional.of(memberProfile));

        assertThatThrownBy(() -> authService.login(info))
                .isInstanceOf(WithdrawnMemberException.class)
                .hasMessage("탈퇴 처리된 계정입니다. 탈퇴일로부터 30일 후 재가입할 수 있습니다.");

        verify(memberProvisioningService, never()).createOAuthMember(any(), any());
        verify(authAccountService, never()).login(any(), any(), any());
    }
}
