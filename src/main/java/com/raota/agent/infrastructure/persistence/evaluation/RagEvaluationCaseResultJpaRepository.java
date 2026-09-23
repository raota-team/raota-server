package com.raota.agent.infrastructure.persistence.evaluation;

import com.raota.agent.application.evaluation.RagEvaluationCaseStatus;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationCaseResultEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RagEvaluationCaseResultJpaRepository extends JpaRepository<RagEvaluationCaseResultEntity, Long> {

    List<RagEvaluationCaseResultEntity> findByRunIdOrderByIdAsc(String runId);

    Optional<RagEvaluationCaseResultEntity> findByRunIdAndCaseId(String runId, String caseId);

    long countByRunIdAndStatus(String runId, RagEvaluationCaseStatus status);

}
