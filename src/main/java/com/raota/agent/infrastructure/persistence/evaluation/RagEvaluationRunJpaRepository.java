package com.raota.agent.infrastructure.persistence.evaluation;

import com.raota.agent.application.evaluation.RagEvaluationStatus;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RagEvaluationRunJpaRepository extends JpaRepository<RagEvaluationRunEntity, String> {
    Optional<RagEvaluationRunEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<RagEvaluationRunEntity> findFirstByStatusInOrderByCreatedAtDesc(Collection<RagEvaluationStatus> statuses);

    @Query("""
            select run
            from RagEvaluationRunEntity run
            where run.createdAt < :createdAt
               or (run.createdAt = :createdAt and run.runId < :runId)
            order by run.createdAt desc, run.runId desc
            """)
    List<RagEvaluationRunEntity> findAfterCursor(
            @Param("createdAt") LocalDateTime createdAt,
            @Param("runId") String runId,
            Pageable pageable
    );

    /**
     * QUEUED 실행만 RUNNING으로 전이한다. 이미 복구·종료된 실행이면 0을 반환한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RagEvaluationRunEntity run
            set run.status = com.raota.agent.application.evaluation.RagEvaluationStatus.RUNNING,
                run.startedAt = :now,
                run.heartbeatAt = :now
            where run.runId = :runId
              and run.status = com.raota.agent.application.evaluation.RagEvaluationStatus.QUEUED
            """)
    int markRunning(@Param("runId") String runId, @Param("now") LocalDateTime now);

    /**
     * RUNNING 실행의 heartbeat만 갱신한다. 0이면 실행이 더 이상 활성 상태가 아니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RagEvaluationRunEntity run
            set run.heartbeatAt = :now
            where run.runId = :runId
              and run.status = com.raota.agent.application.evaluation.RagEvaluationStatus.RUNNING
            """)
    int touchHeartbeat(@Param("runId") String runId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RagEvaluationRunEntity run
            set run.status = com.raota.agent.application.evaluation.RagEvaluationStatus.REVIEW_REQUIRED,
                run.aggregateMetrics = :aggregateMetrics,
                run.completedAt = :now,
                run.activeSlot = null
            where run.runId = :runId
              and run.status = com.raota.agent.application.evaluation.RagEvaluationStatus.RUNNING
            """)
    int markReviewRequired(
            @Param("runId") String runId,
            @Param("aggregateMetrics") String aggregateMetrics,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RagEvaluationRunEntity run
            set run.status = com.raota.agent.application.evaluation.RagEvaluationStatus.FAILED,
                run.fatalError = :fatalError,
                run.completedAt = :now,
                run.activeSlot = null
            where run.runId = :runId
              and run.status in (
                  com.raota.agent.application.evaluation.RagEvaluationStatus.QUEUED,
                  com.raota.agent.application.evaluation.RagEvaluationStatus.RUNNING
              )
            """)
    int markFailed(
            @Param("runId") String runId,
            @Param("fatalError") String fatalError,
            @Param("now") LocalDateTime now
    );

    /**
     * heartbeat가 임계 시각보다 오래된 활성 실행을 FAILED로 전이한다.
     * 조건부 단일 UPDATE이므로 여러 서버가 동시에 실행하거나 반복 실행해도 결과가 같다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RagEvaluationRunEntity run
            set run.status = com.raota.agent.application.evaluation.RagEvaluationStatus.FAILED,
                run.fatalError = :fatalError,
                run.completedAt = :now,
                run.activeSlot = null
            where run.status in (
                  com.raota.agent.application.evaluation.RagEvaluationStatus.QUEUED,
                  com.raota.agent.application.evaluation.RagEvaluationStatus.RUNNING
              )
              and (run.heartbeatAt is null or run.heartbeatAt < :threshold)
            """)
    int failStale(
            @Param("threshold") LocalDateTime threshold,
            @Param("fatalError") String fatalError,
            @Param("now") LocalDateTime now
    );
}
