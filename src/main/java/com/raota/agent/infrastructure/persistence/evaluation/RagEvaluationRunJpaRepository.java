package com.raota.agent.infrastructure.persistence.evaluation;

import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import java.util.Optional;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.raota.agent.application.evaluation.RagEvaluationStatus;

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
}
