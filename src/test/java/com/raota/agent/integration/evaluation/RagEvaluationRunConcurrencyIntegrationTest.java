package com.raota.agent.integration.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.raota.agent.application.evaluation.RagEvaluationAlreadyRunningException;
import com.raota.agent.application.evaluation.RagEvaluationRunService;
import com.raota.agent.application.evaluation.RagEvaluationRunService.RunStart;
import com.raota.agent.application.evaluation.RagEvaluationRunner;
import com.raota.agent.application.evaluation.RagEvaluationSplit;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationCaseResultJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockClient.LockToken;
import com.raota.global.redis.RedisLockUnavailableException;
import com.raota.support.BaseIntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 서비스에 JVM 동기화가 없으므로 스레드 동시 호출이 여러 서버의 동시 요청을 재현한다.
 */
class RagEvaluationRunConcurrencyIntegrationTest extends BaseIntegrationTest {

    private static final String LOCK_KEY = "lock:rag-evaluation:active";

    @MockitoBean
    private RagEvaluationRunner runner;

    @MockitoSpyBean
    private RedisLockClient lockClient;

    @Autowired
    private RagEvaluationRunService runService;

    @Autowired
    private RagEvaluationRunJpaRepository runRepository;

    @Autowired
    private RagEvaluationCaseResultJpaRepository caseRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
        caseRepository.deleteAllInBatch();
        runRepository.deleteAllInBatch();
        redisTemplate.delete(LOCK_KEY);
    }

    @Test
    @DisplayName("서로 다른 멱등키로 동시에 요청하면 하나만 예약되고 나머지는 이미 실행 중 오류를 받는다")
    void concurrentRequestsWithDifferentKeysReserveOnlyOne() throws Exception {
        List<Outcome> outcomes = concurrently(20, index -> UUID.randomUUID().toString());

        assertThat(outcomes.stream().filter(Outcome::started)).hasSize(1);
        assertThat(outcomes.stream().filter(Outcome::alreadyRunning)).hasSize(19);
        assertThat(runRepository.count()).isEqualTo(1);
        verify(runner, times(1)).execute(anyString(), any(), any());
        assertThat(redisTemplate.hasKey(LOCK_KEY)).isFalse();
    }

    @Test
    @DisplayName("같은 멱등키로 동시에 요청하면 모두 같은 실행을 받는다")
    void concurrentRequestsWithSameKeyReturnSameRun() throws Exception {
        String key = UUID.randomUUID().toString();

        List<Outcome> outcomes = concurrently(10, index -> key);

        assertThat(outcomes).allMatch(Outcome::started);
        assertThat(outcomes.stream().map(outcome -> outcome.start().runId()).distinct()).hasSize(1);
        assertThat(outcomes.stream().filter(outcome -> !outcome.start().idempotentReplay())).hasSize(1);
        assertThat(runRepository.count()).isEqualTo(1);
        verify(runner, times(1)).execute(anyString(), any(), any());
    }

    @Test
    @DisplayName("Redis 락이 모두에게 획득된 것처럼 동작해도 DB 활성 슬롯이 하나만 허용한다")
    void databaseGuardHoldsWhenRedisLockDoesNotExclude() throws Exception {
        doReturn(Optional.of(new LockToken(LOCK_KEY, "broken"))).when(lockClient)
            .tryAcquire(anyString(), any(Duration.class));
        doReturn(false).when(lockClient).release(any());

        List<Outcome> outcomes = concurrently(20, index -> UUID.randomUUID().toString());

        assertThat(outcomes.stream().filter(Outcome::started)).hasSize(1);
        assertThat(outcomes.stream().filter(Outcome::alreadyRunning)).hasSize(19);
        assertThat(runRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Redis에 접근할 수 없어도 DB 제약만으로 예약하고 중복은 거절한다")
    void reservesWithDatabaseGuardWhenRedisUnavailable() {
        doThrow(new RedisLockUnavailableException("down", new RuntimeException())).when(lockClient)
            .tryAcquire(anyString(), any(Duration.class));

        RunStart first = start(UUID.randomUUID().toString());

        assertThat(first.idempotentReplay()).isFalse();
        assertThatThrownBy(() -> start(UUID.randomUUID().toString()))
            .isInstanceOf(RagEvaluationAlreadyRunningException.class);
    }

    @Test
    @DisplayName("다른 서버가 예약 락을 오래 보유하면 대기 후 이미 실행 중 오류를 반환한다")
    void lockHeldByAnotherServerTimesOut() {
        redisTemplate.opsForValue().set(LOCK_KEY, "other-server", Duration.ofSeconds(30));

        assertThatThrownBy(() -> start(UUID.randomUUID().toString()))
            .isInstanceOf(RagEvaluationAlreadyRunningException.class);
        assertThat(runRepository.count()).isZero();
        assertThat(redisTemplate.opsForValue().get(LOCK_KEY)).isEqualTo("other-server");
    }

    @Test
    @DisplayName("활성 실행이 끝나면 새 멱등키로 다시 예약할 수 있다")
    void canReserveAgainAfterActiveRunFinishes() {
        RunStart first = start(UUID.randomUUID().toString());
        assertThatThrownBy(() -> start(UUID.randomUUID().toString()))
            .isInstanceOf(RagEvaluationAlreadyRunningException.class);

        transactionTemplate.execute(status -> runRepository.markRunning(first.runId(), LocalDateTime.now()));
        transactionTemplate
            .execute(status -> runRepository.markReviewRequired(first.runId(), "{}", LocalDateTime.now()));

        RunStart second = start(UUID.randomUUID().toString());
        assertThat(second.runId()).isNotEqualTo(first.runId());
    }

    @Test
    @DisplayName("같은 멱등키로 다른 분할을 요청하면 거절한다")
    void rejectsSameKeyWithDifferentRequest() {
        String key = UUID.randomUUID().toString();
        start(key);

        assertThatThrownBy(() -> runService.start(null, RagEvaluationSplit.HOLDOUT, key))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private RunStart start(String key) {
        return runService.start(null, RagEvaluationSplit.DEV, key);
    }

    private List<Outcome> concurrently(int count, IntFunction<String> keyForIndex) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(count)) {
            for (int index = 0; index < count; index++) {
                String key = keyForIndex.apply(index);
                Callable<Outcome> task = () -> {
                    ready.await();
                    try {
                        return new Outcome(start(key), null);
                    }
                    catch (RuntimeException exception) {
                        return new Outcome(null, exception);
                    }
                };
                futures.add(executor.submit(task));
            }
            ready.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(get(future));
            }
            return outcomes;
        }
    }

    private static Outcome get(Future<Outcome> future) throws InterruptedException {
        try {
            return future.get();
        }
        catch (ExecutionException exception) {
            throw new IllegalStateException(exception.getCause());
        }
    }

    private record Outcome(RunStart start, RuntimeException error) {
        boolean started() {
            return start != null;
        }

        boolean alreadyRunning() {
            return error instanceof RagEvaluationAlreadyRunningException;
        }
    }

}
