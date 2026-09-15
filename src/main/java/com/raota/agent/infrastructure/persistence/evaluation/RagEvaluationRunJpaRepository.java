package com.raota.agent.infrastructure.persistence.evaluation;

import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import com.raota.agent.application.evaluation.RagEvaluationStatus;

public interface RagEvaluationRunJpaRepository extends JpaRepository<RagEvaluationRunEntity, String> {
    Optional<RagEvaluationRunEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<RagEvaluationRunEntity> findFirstByStatusInOrderByCreatedAtDesc(Collection<RagEvaluationStatus> statuses);
}
