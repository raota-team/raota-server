package com.raota.mobile.account.application.service;

import com.raota.mobile.account.application.command.SocialLoginCommand;
import com.raota.mobile.account.application.port.AccessTokenIssuer;
import com.raota.mobile.account.application.port.RefreshTokenStore;
import com.raota.mobile.account.application.port.SocialTokenVerifier;
import com.raota.mobile.account.application.result.MobileLoginResult;
import com.raota.mobile.account.application.result.MobileMemberResult;
import com.raota.mobile.account.application.result.SocialIdentity;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserOAuthAccount;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.account.domain.repository.MobileUserOAuthAccountRepository;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 소셜 계정과 회원을 연결하고 기기별 토큰을 발급한다. */
@Service
public class MobileAuthService {

    private final List<SocialTokenVerifier> verifiers;

    private final MobileUserOAuthAccountRepository accounts;

    private final MobileUserRepository users;

    private final AccessTokenIssuer accessTokens;

    private final RefreshTokenStore refreshTokens;

    private final TransactionTemplate accountTransaction;

    public MobileAuthService(List<SocialTokenVerifier> verifiers, MobileUserOAuthAccountRepository accounts,
            MobileUserRepository users, AccessTokenIssuer accessTokens, RefreshTokenStore refreshTokens,
            PlatformTransactionManager transactionManager) {
        this.verifiers = List.copyOf(verifiers);
        this.accounts = accounts;
        this.users = users;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.accountTransaction = new TransactionTemplate(transactionManager);
        this.accountTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public MobileLoginResult login(SocialLoginCommand command) {
        SocialTokenVerifier verifier = verifierFor(command.provider());
        SocialIdentity identity = verifier.verify(command.credential());
        Instant now = Instant.now();
        LoginMember loginMember = existingMember(identity, now).orElseGet(() -> createOrReadMember(identity, now));
        Long userId = loginMember.userId();
        return new MobileLoginResult(accessTokens.issue(userId), refreshTokens.issue(userId),
                accessTokens.expiresInSeconds(), loginMember.isNewMember(), loginMember.member());
    }

    private SocialTokenVerifier verifierFor(OAuthProvider provider) {
        for (SocialTokenVerifier verifier : verifiers) {
            if (verifier.provider() == provider) {
                return verifier;
            }
        }
        throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "지원하지 않는 로그인 제공자입니다.");
    }

    private Optional<LoginMember> existingMember(SocialIdentity identity, Instant now) {
        return accountTransaction
            .execute(status -> accounts.findByProviderAndProviderSubject(identity.provider(), identity.subject())
                .map(account -> {
                    MobileUser user = availableUser(account.getUserId());
                    account.recordLogin(now);
                    return new LoginMember(user.getId(), false, MobileMemberResult.from(user));
                }));
    }

    private LoginMember createOrReadMember(SocialIdentity identity, Instant now) {
        try {
            return accountTransaction.execute(status -> {
                MobileUser user = users.saveAndFlush(MobileUser.onboarding(identity.email()));
                accounts.saveAndFlush(MobileUserOAuthAccount.link(user.getId(), identity.provider(), identity.subject(),
                        identity.email(), now));
                return new LoginMember(user.getId(), true, MobileMemberResult.from(user));
            });
        }
        catch (DataIntegrityViolationException exception) {
            return existingMember(identity, now).orElseThrow(() -> exception);
        }
    }

    private MobileUser availableUser(Long userId) {
        MobileUser user = users.findById(userId)
            .orElseThrow(() -> new MobileException(MobileErrorCode.UNAUTHORIZED, "사용할 수 없는 계정입니다."));
        if (user.getStatus() != MobileUserStatus.ONBOARDING && user.getStatus() != MobileUserStatus.ACTIVE) {
            throw new MobileException(MobileErrorCode.UNAUTHORIZED, "사용할 수 없는 계정입니다.");
        }
        return user;
    }

    private record LoginMember(Long userId, boolean isNewMember, MobileMemberResult member) {
    }

}
