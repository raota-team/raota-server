package com.raota.mobile.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.application.command.SocialLoginCommand;
import com.raota.mobile.account.application.port.RefreshTokenStore;
import com.raota.mobile.account.application.result.SocialIdentity;
import com.raota.mobile.account.domain.model.ConsentType;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserConsent;
import com.raota.mobile.account.domain.model.MobileUserOAuthAccount;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.account.domain.repository.MobileUserConsentRepository;
import com.raota.mobile.account.domain.repository.MobileUserOAuthAccountRepository;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.external.KakaoAccessTokenVerifier;
import com.raota.support.BaseIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class MobileWithdrawalPurgeIntegrationTest extends BaseIntegrationTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private MobileWithdrawalPurgeService purges;

    @Autowired
    private MobileAuthService auth;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileUserOAuthAccountRepository accounts;

    @Autowired
    private MobileUserConsentRepository consents;

    @Autowired
    private RefreshTokenStore refreshTokens;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private KakaoAccessTokenVerifier kakaoVerifier;

    private final List<Long> createdUsers = new ArrayList<>();

    private String subject;

    @BeforeEach
    void setUp() {
        subject = UUID.randomUUID().toString();
        when(kakaoVerifier.provider()).thenReturn(OAuthProvider.KAKAO);
        when(kakaoVerifier.verify(any(SocialCredential.class)))
            .thenAnswer(invocation -> new SocialIdentity(OAuthProvider.KAKAO, subject, "new@example.com"));
    }

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (Long userId : createdUsers) {
                refreshTokens.revokeAll(userId);
                consents.deleteByUserId(userId);
                accounts.deleteByUserId(userId);
                users.deleteById(userId);
            }
        });
        createdUsers.clear();
    }

    @Test
    void 기한이_지난_회원만_동의와_소셜_계정을_지우고_프로필을_익명화한다() {
        Long due = createUser(OAuthProvider.KAKAO, subject, true, REQUESTED_AT);
        Long notDue = createUser(OAuthProvider.GOOGLE, UUID.randomUUID().toString(), false,
                REQUESTED_AT.plus(Duration.ofDays(10)));
        Long active = createUser(OAuthProvider.GOOGLE, UUID.randomUUID().toString(), true, null);

        assertThat(purges.purgeExpired(REQUESTED_AT.plus(Duration.ofDays(30)))).isEqualTo(1);

        MobileUser anonymized = users.findById(due).orElseThrow();
        assertThat(anonymized.getStatus()).isEqualTo(MobileUserStatus.WITHDRAWN);
        assertThat(anonymized.getEmail()).isNull();
        assertThat(anonymized.getNickname()).isNull();
        assertThat(anonymized.getNicknameNormalized()).isNull();
        assertThat(anonymized.getAvatarUrl()).isNull();
        assertThat(anonymized.getBio()).isNull();
        assertThat(anonymized.getFavoriteRamenType()).isNull();
        assertThat(anonymized.getPurgeScheduledAt()).isNull();
        assertThat(anonymized.getWithdrawalRequestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(accountCount(due)).isZero();
        assertThat(consentCount(due)).isZero();

        assertThat(users.findById(notDue).orElseThrow().getStatus()).isEqualTo(MobileUserStatus.WITHDRAW_PENDING);
        assertThat(accountCount(notDue)).isEqualTo(1);
        assertThat(consentCount(notDue)).isEqualTo(1);
        assertThat(users.findById(active).orElseThrow().getStatus()).isEqualTo(MobileUserStatus.ACTIVE);
        assertThat(accountCount(active)).isEqualTo(1);
        assertThat(consentCount(active)).isEqualTo(1);
    }

    @Test
    void 기한이_지난_회원이_한_번에_읽는_수보다_많아도_모두_처리한다() {
        for (int index = 0; index < 100; index++) {
            createUser(OAuthProvider.KAKAO, UUID.randomUUID().toString(), false, REQUESTED_AT);
        }
        Long secondPage = createUser(OAuthProvider.KAKAO, UUID.randomUUID().toString(), false, REQUESTED_AT);

        assertThat(purges.purgeExpired(REQUESTED_AT.plus(Duration.ofDays(30)))).isEqualTo(101);
        assertThat(users.findById(secondPage).orElseThrow().getStatus()).isEqualTo(MobileUserStatus.WITHDRAWN);
        assertThat(accountCount(secondPage)).isZero();
    }

    @Test
    void 익명화된_제공자_계정으로_다시_로그인하면_새_회원을_만든다() {
        Long previous = createUser(OAuthProvider.KAKAO, subject, false, REQUESTED_AT);
        assertThat(purges.purgeExpired(REQUESTED_AT.plus(Duration.ofDays(30)))).isEqualTo(1);

        var login = auth.login(new SocialLoginCommand(OAuthProvider.KAKAO,
                new SocialCredential(null, "valid-kakao-token", null, null)));
        Long next = Long.valueOf(login.member().id());
        createdUsers.add(next);
        assertThat(login.isNewMember()).isTrue();
        assertThat(next).isNotEqualTo(previous);
        assertThat(login.member().status()).isEqualTo("ONBOARDING");
        assertThat(accounts.findByProviderAndProviderSubject(OAuthProvider.KAKAO, subject).orElseThrow().getUserId())
            .isEqualTo(next);
    }

    private Long createUser(OAuthProvider provider, String providerSubject, boolean active, Instant requestedAt) {
        MobileUser user = MobileUser.onboarding("original@example.com");
        user.updateProfile(null, "https://example.com/avatar.png", "라멘 좋아요", "돈코츠");
        if (active) {
            user.completeOnboarding(Nickname.of("Member_" + createdUsers.size()), REQUESTED_AT);
        }
        if (requestedAt != null) {
            user.requestWithdrawal(requestedAt, Duration.ofDays(30));
        }
        Long userId = users.saveAndFlush(user).getId();
        createdUsers.add(userId);
        accounts.saveAndFlush(
                MobileUserOAuthAccount.link(userId, provider, providerSubject, "original@example.com", REQUESTED_AT));
        consents.saveAndFlush(MobileUserConsent.decide(userId, ConsentType.TERMS, "2026-09-01", true, REQUESTED_AT));
        return userId;
    }

    private long accountCount(Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_user_oauth_account WHERE user_id = ?", Long.class,
                userId);
    }

    private long consentCount(Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_user_consent WHERE user_id = ?", Long.class, userId);
    }

}
