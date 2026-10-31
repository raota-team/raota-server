package com.raota.mobile.account.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockClient.LockToken;
import com.raota.global.redis.RedisLockUnavailableException;
import com.raota.mobile.account.application.service.MobileWithdrawalPurgeService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileAccountPurgeSchedulerTest {

    @Mock
    private MobileWithdrawalPurgeService purgeService;

    @Mock
    private RedisLockClient locks;

    @InjectMocks
    private MobileAccountPurgeScheduler scheduler;

    @Test
    void 락을_잡으면_익명화한다() {
        given(locks.tryAcquire(eq(MobileAccountPurgeScheduler.LOCK_KEY), any()))
                .willReturn(Optional.of(new LockToken(MobileAccountPurgeScheduler.LOCK_KEY, "token")));

        scheduler.purgeExpiredMembers();

        verify(purgeService).purgeExpired(any());
    }

    @Test
    void 다른_서버가_락을_잡고_있으면_건너뛴다() {
        given(locks.tryAcquire(eq(MobileAccountPurgeScheduler.LOCK_KEY), any())).willReturn(Optional.empty());

        scheduler.purgeExpiredMembers();

        verify(purgeService, never()).purgeExpired(any());
    }

    @Test
    void Redis에_접근할_수_없으면_건너뛴다() {
        given(locks.tryAcquire(eq(MobileAccountPurgeScheduler.LOCK_KEY), any()))
                .willThrow(new RedisLockUnavailableException("down", new RuntimeException()));

        scheduler.purgeExpiredMembers();

        verify(purgeService, never()).purgeExpired(any());
    }
}
