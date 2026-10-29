package com.raota.mobile.account.application.service;

import com.raota.mobile.account.application.port.AppleTokenClient;
import com.raota.mobile.account.application.port.AppleTokenRevocationRejectedException;
import com.raota.mobile.account.application.port.ProviderTokenCipher;
import com.raota.mobile.account.domain.event.MobileMemberPurgedEvent;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserOAuthAccount;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.account.domain.repository.MobileUserConsentRepository;
import com.raota.mobile.account.domain.repository.MobileUserOAuthAccountRepository;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 탈퇴 유예가 끝난 회원의 외부 토큰과 식별 데이터를 회원별로 정리한다.
 *
 * <p>
 * Apple 토큰 폐기가 실패해도 개인정보 삭제를 무기한 미루지 않는다. Apple이 거절했거나(4xx) 저장된 토큰을 복호화할 수 없으면 다시 시도해도
 * 소용없으므로 바로 익명화한다. 5xx·연결 실패는 다음 날 다시 시도하되, 삭제 예정 시각에서 {@link #APPLE_REVOKE_RETRY_LIMIT}가 지나면
 * 폐기 없이 익명화한다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileWithdrawalPurgeService {

    private static final int PAGE_SIZE = 100;

    /** Apple 장애가 이 기간 넘게 이어지면 토큰 폐기를 포기하고 익명화한다. */
    static final Duration APPLE_REVOKE_RETRY_LIMIT = Duration.ofDays(7);

    private final MobileUserRepository userRepository;

    private final MobileUserOAuthAccountRepository oauthAccountRepository;

    private final MobileUserConsentRepository consentRepository;

    private final AppleTokenClient appleTokenClient;

    private final ProviderTokenCipher providerTokenCipher;

    private final PlatformTransactionManager transactionManager;

    private final ApplicationEventPublisher eventPublisher;

    public int purgeExpired(Instant now) {
        int purged = 0;
        Instant afterScheduledAt = null;
        Long afterId = null;
        var pageSize = PageRequest.of(0, PAGE_SIZE);
        while (true) {
            List<MobileUserRepository.DueUser> due;
            if (afterScheduledAt == null) {
                due = userRepository.findDueForPurge(MobileUserStatus.WITHDRAW_PENDING, now, pageSize);
            } else {
                due = userRepository.findDueForPurgeAfter(
                        MobileUserStatus.WITHDRAW_PENDING, now, afterScheduledAt, afterId, pageSize);
            }
            for (MobileUserRepository.DueUser candidate : due) {
                Long userId = candidate.getId();
                afterScheduledAt = candidate.getPurgeScheduledAt();
                afterId = userId;
                try {
                    if (!revokeAppleTokens(userId, candidate.getPurgeScheduledAt(), now)) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(requiresNewTransaction().execute(status -> anonymize(userId, now)))) {
                        purged++;
                    }
                } catch (Exception exception) {
                    log.error("모바일 탈퇴 회원 익명화 실패: userId={}", userId, exception);
                }
            }
            if (due.size() < PAGE_SIZE) {
                return purged;
            }
        }
    }

    /** 익명화를 진행해도 되면 {@code true}, 일시적인 Apple 장애라 다음 실행으로 미뤄야 하면 {@code false}다. */
    private boolean revokeAppleTokens(Long userId, Instant purgeScheduledAt, Instant now) {
        for (MobileUserOAuthAccount account : oauthAccountRepository.findAllByUserId(userId)) {
            if (account.getProvider() != OAuthProvider.APPLE || account.getAppleRefreshTokenEncrypted() == null) {
                continue;
            }
            String refreshToken;
            try {
                refreshToken = providerTokenCipher.decrypt(account.getAppleRefreshTokenEncrypted());
            } catch (RuntimeException exception) {
                log.error("모바일 탈퇴: Apple 토큰을 복호화할 수 없어 폐기 없이 익명화한다. userId={}", userId, exception);
                continue;
            }
            try {
                appleTokenClient.revoke(refreshToken);
            } catch (AppleTokenRevocationRejectedException exception) {
                log.error("모바일 탈퇴: Apple이 토큰 폐기를 거절해 폐기 없이 익명화한다. userId={}", userId, exception);
            } catch (RuntimeException exception) {
                if (now.isBefore(purgeScheduledAt.plus(APPLE_REVOKE_RETRY_LIMIT))) {
                    log.warn("모바일 탈퇴: Apple 토큰 폐기 실패, 다음 실행에 다시 시도한다. userId={}", userId, exception);
                    return false;
                }
                log.error(
                        "모바일 탈퇴: Apple 토큰 폐기가 {} 넘게 실패해 폐기 없이 익명화한다. userId={}",
                        APPLE_REVOKE_RETRY_LIMIT,
                        userId,
                        exception);
            }
        }
        return true;
    }

    private boolean anonymize(Long userId, Instant now) {
        MobileUser user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null
                || user.getStatus() != MobileUserStatus.WITHDRAW_PENDING
                || user.getPurgeScheduledAt() == null
                || user.getPurgeScheduledAt().isAfter(now)) {
            return false;
        }
        consentRepository.deleteByUserId(userId);
        oauthAccountRepository.deleteByUserId(userId);
        user.anonymize();
        eventPublisher.publishEvent(new MobileMemberPurgedEvent(userId));
        return true;
    }

    /** 회원마다 따로 커밋해 한 회원의 실패가 다른 회원의 익명화를 되돌리지 않게 한다. */
    private TransactionTemplate requiresNewTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
