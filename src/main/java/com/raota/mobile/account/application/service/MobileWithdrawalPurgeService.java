package com.raota.mobile.account.application.service;

import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.repository.MobileUserConsentRepository;
import com.raota.mobile.account.domain.repository.MobileUserOAuthAccountRepository;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import java.util.List;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 탈퇴 유예가 끝난 회원의 식별 데이터를 회원별로 정리한다. */
@Slf4j
@Service
public class MobileWithdrawalPurgeService {

    private final MobileUserRepository users;

    private final MobileUserOAuthAccountRepository accounts;

    private final MobileUserConsentRepository consents;

    private final TransactionTemplate purgeTransaction;

    private static final int PAGE_SIZE = 100;

    public MobileWithdrawalPurgeService(MobileUserRepository users, MobileUserOAuthAccountRepository accounts,
            MobileUserConsentRepository consents, PlatformTransactionManager transactionManager) {
        this.users = users;
        this.accounts = accounts;
        this.consents = consents;
        this.purgeTransaction = new TransactionTemplate(transactionManager);
        this.purgeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int purgeExpired(Instant now) {
        int purged = 0;
        Instant afterScheduledAt = null;
        Long afterId = null;
        var pageSize = PageRequest.of(0, PAGE_SIZE);
        while (true) {
            List<MobileUserRepository.DueUser> due = afterScheduledAt == null
                    ? users.findDueForPurge(MobileUserStatus.WITHDRAW_PENDING, now, pageSize)
                    : users.findDueForPurgeAfter(MobileUserStatus.WITHDRAW_PENDING, now, afterScheduledAt, afterId,
                            pageSize);
            for (MobileUserRepository.DueUser candidate : due) {
                Long userId = candidate.getId();
                afterScheduledAt = candidate.getPurgeScheduledAt();
                afterId = userId;
                try {
                    if (Boolean.TRUE.equals(purgeTransaction.execute(status -> anonymize(userId, now)))) {
                        purged++;
                    }
                }
                catch (Exception exception) {
                    log.error("모바일 탈퇴 회원 익명화 실패: userId={}", userId, exception);
                }
            }
            if (due.size() < PAGE_SIZE) {
                return purged;
            }
        }
    }

    private boolean anonymize(Long userId, Instant now) {
        MobileUser user = users.findByIdForUpdate(userId).orElse(null);
        if (user == null || user.getStatus() != MobileUserStatus.WITHDRAW_PENDING || user.getPurgeScheduledAt() == null
                || user.getPurgeScheduledAt().isAfter(now)) {
            return false;
        }
        consents.deleteByUserId(userId);
        accounts.deleteByUserId(userId);
        user.anonymize();
        return true;
    }

}
