package com.raota.mobile.account.application.service;

import com.raota.mobile.account.application.config.MobileAccountProperties;
import com.raota.mobile.account.application.port.RefreshTokenStore;
import com.raota.mobile.account.application.command.MobileOnboardingCommand;
import com.raota.mobile.account.application.result.MobileMemberResult;
import com.raota.mobile.account.application.result.MobileNicknameAvailabilityResult;
import com.raota.mobile.account.application.result.MobileWithdrawalResult;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.ConsentType;
import com.raota.mobile.account.domain.model.MobileUserConsent;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.domain.repository.MobileUserConsentRepository;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 인증된 모바일 회원의 프로필을 조회하고 수정한다. */
@Service
public class MobileMemberService {

    private final MobileUserRepository users;

    private final MobileUserConsentRepository consents;

    private final RefreshTokenStore refreshTokens;

    private final MobileAccountProperties accountProperties;

    private final TransactionTemplate accountTransaction;

    public MobileMemberService(MobileUserRepository users, MobileUserConsentRepository consents,
            PlatformTransactionManager transactionManager, RefreshTokenStore refreshTokens,
            MobileAccountProperties accountProperties) {
        this.users = users;
        this.consents = consents;
        this.refreshTokens = refreshTokens;
        this.accountProperties = accountProperties;
        this.accountTransaction = new TransactionTemplate(transactionManager);
        this.accountTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public MobileMemberResult me(Long userId) {
        return MobileMemberResult.from(availableUser(userId));
    }

    @Transactional
    public MobileMemberResult updateProfile(Long userId, String email, String avatarUrl, String bio,
            String favoriteRamenType) {
        MobileUser user = availableUser(users.findByIdForUpdate(userId));
        user.updateProfile(email, avatarUrl, bio, favoriteRamenType);
        return MobileMemberResult.from(user);
    }

    @Transactional(readOnly = true)
    public MobileNicknameAvailabilityResult nicknameAvailability(Long userId, String rawNickname) {
        availableUser(userId);
        Nickname nickname = Nickname.of(rawNickname);
        return new MobileNicknameAvailabilityResult(nickname.display(),
                !users.existsByNicknameNormalizedAndIdNot(nickname.normalized(), userId));
    }

    public MobileMemberResult onboard(Long userId, MobileOnboardingCommand command) {
        try {
            return accountTransaction.execute(status -> {
                MobileUser user = availableUser(users.findByIdForUpdate(userId));
                if (user.getStatus() == MobileUserStatus.ACTIVE) {
                    throw new MobileException(MobileErrorCode.CONFLICT, "이미 온보딩을 마쳤습니다.");
                }
                Nickname nickname = Nickname.of(command.nickname());
                validateConsents(command.consents());
                if (users.existsByNicknameNormalizedAndIdNot(nickname.normalized(), userId)) {
                    throw new MobileException(MobileErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다.");
                }

                Instant now = Instant.now();
                user.completeOnboarding(nickname, now);
                users.saveAndFlush(user);
                consents.saveAll(command.consents()
                    .stream()
                    .map(decision -> MobileUserConsent.decide(userId, decision.type(), decision.documentVersion(),
                            decision.granted(), now))
                    .toList());
                return MobileMemberResult.from(user);
            });
        }
        catch (DataIntegrityViolationException exception) {
            String message = exception.getMostSpecificCause().getMessage();
            if (message != null && message.contains("uk_v2_user_nickname_normalized")) {
                throw new MobileException(MobileErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다.");
            }
            throw exception;
        }
    }

    public MobileWithdrawalResult requestWithdrawal(Long userId) {
        MobileWithdrawalResult result = accountTransaction.execute(status -> {
            MobileUser user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.UNAUTHORIZED, "사용할 수 없는 계정입니다."));
            // 응답의 삭제 예정 시각이 DATETIME(6)에 저장되는 값과 같도록 마이크로초로 자른다.
            user.requestWithdrawal(Instant.now().truncatedTo(ChronoUnit.MICROS),
                    accountProperties.withdrawalGracePeriod());
            return new MobileWithdrawalResult(user.getStatus(), user.getPurgeScheduledAt());
        });
        refreshTokens.revokeAll(userId);
        return result;
    }

    private void validateConsents(List<MobileOnboardingCommand.ConsentDecision> decisions) {
        EnumSet<ConsentType> seen = EnumSet.noneOf(ConsentType.class);
        EnumSet<ConsentType> granted = EnumSet.noneOf(ConsentType.class);
        for (MobileOnboardingCommand.ConsentDecision decision : decisions) {
            if (!seen.add(decision.type())) {
                throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "동의 유형은 한 번만 제출할 수 있습니다.");
            }
            if (decision.granted()) {
                granted.add(decision.type());
            }
        }
        if (!granted.contains(ConsentType.TERMS) || !granted.contains(ConsentType.PRIVACY)) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "이용약관과 개인정보 처리방침에 동의해야 합니다.");
        }
    }

    private MobileUser availableUser(Long userId) {
        return availableUser(users.findById(userId));
    }

    private MobileUser availableUser(Optional<MobileUser> found) {
        MobileUser user = found.orElseThrow(() -> new MobileException(MobileErrorCode.UNAUTHORIZED, "사용할 수 없는 계정입니다."));
        if (user.getStatus() != MobileUserStatus.ONBOARDING && user.getStatus() != MobileUserStatus.ACTIVE) {
            throw new MobileException(MobileErrorCode.UNAUTHORIZED, "사용할 수 없는 계정입니다.");
        }
        return user;
    }

}
