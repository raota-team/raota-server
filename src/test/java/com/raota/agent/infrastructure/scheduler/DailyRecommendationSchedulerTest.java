package com.raota.agent.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.raota.agent.application.recommendation.DailyCurationService;
import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockClient.LockToken;
import com.raota.global.redis.RedisLockUnavailableException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyRecommendationSchedulerTest {

    @Mock
    private DailyCurationService dailyCurationService;

    @Mock
    private RedisLockClient locks;

    @InjectMocks
    private DailyRecommendationScheduler scheduler;

    @Test
    void 락을_잡으면_큐레이션을_만든다() {
        given(locks.tryAcquire(eq(DailyRecommendationScheduler.LOCK_KEY), any()))
                .willReturn(Optional.of(new LockToken(DailyRecommendationScheduler.LOCK_KEY, "token")));

        scheduler.generateDailyRamenRecommendation();

        verify(dailyCurationService).generateDailyCuration();
    }

    @Test
    void 다른_서버가_락을_잡고_있으면_건너뛴다() {
        given(locks.tryAcquire(eq(DailyRecommendationScheduler.LOCK_KEY), any()))
                .willReturn(Optional.empty());

        scheduler.generateDailyRamenRecommendation();

        verify(dailyCurationService, never()).generateDailyCuration();
    }

    @Test
    void Redis에_접근할_수_없으면_락_없이_만든다() {
        given(locks.tryAcquire(eq(DailyRecommendationScheduler.LOCK_KEY), any()))
                .willThrow(new RedisLockUnavailableException("down", new RuntimeException()));

        scheduler.generateDailyRamenRecommendation();

        verify(dailyCurationService).generateDailyCuration();
    }
}
