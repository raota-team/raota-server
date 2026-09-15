package com.raota.agent.infrastructure.persistence.evaluation;

import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RagEvaluationRunJpaRepository extends JpaRepository<RagEvaluationRunEntity, String> {
    Optional<RagEvaluationRunEntity> findByIdempotencyKey(String idempotencyKey);
}
