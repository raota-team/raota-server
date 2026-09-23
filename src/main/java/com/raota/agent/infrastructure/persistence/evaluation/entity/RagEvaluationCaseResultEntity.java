package com.raota.agent.infrastructure.persistence.evaluation.entity;

import com.raota.agent.application.evaluation.RagEvaluationCaseStatus;
import com.raota.agent.application.evaluation.RagEvaluationCaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_rag_evaluation_case_result",
        uniqueConstraints = @UniqueConstraint(name = "uk_rag_eval_case_run_case",
                columnNames = { "run_id", "case_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagEvaluationCaseResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", length = 36, nullable = false)
    private String runId;

    @Column(name = "case_id", length = 100, nullable = false)
    private String caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", length = 20, nullable = false)
    private RagEvaluationCaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RagEvaluationCaseStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_json", columnDefinition = "json", nullable = false)
    private String requestJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_json", columnDefinition = "json", nullable = false)
    private String expectedJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", columnDefinition = "json")
    private String responseJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_json", columnDefinition = "json")
    private String evidenceJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_json", columnDefinition = "json")
    private String metricsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auto_judgement_json", columnDefinition = "json")
    private String autoJudgementJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_review_json", columnDefinition = "json")
    private String finalReviewJson;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_type", length = 120)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "reviewer_member_id")
    private Long reviewerMemberId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RagEvaluationCaseResultEntity(String runId, String caseId, RagEvaluationCaseType caseType,
            RagEvaluationCaseStatus status, String requestJson, String expectedJson) {
        this.runId = runId;
        this.caseId = caseId;
        this.caseType = caseType;
        this.status = status;
        this.requestJson = requestJson;
        this.expectedJson = expectedJson;
    }

    public static RagEvaluationCaseResultEntity pending(String runId, String caseId, RagEvaluationCaseType caseType,
            String requestJson, String expectedJson) {
        return new RagEvaluationCaseResultEntity(runId, caseId, caseType, RagEvaluationCaseStatus.PENDING, requestJson,
                expectedJson);
    }

    public void markRunning() {
        this.status = RagEvaluationCaseStatus.RUNNING;
    }

    public void recordExecution(RagEvaluationCaseStatus status, String responseJson, String evidenceJson,
            String metricsJson, String autoJudgementJson, Long latencyMs, String errorType, String errorMessage) {
        this.status = status;
        this.responseJson = responseJson;
        this.evidenceJson = evidenceJson;
        this.metricsJson = metricsJson;
        this.autoJudgementJson = autoJudgementJson;
        this.latencyMs = latencyMs;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public void recordReview(Long reviewerMemberId, String finalReviewJson) {
        this.reviewerMemberId = reviewerMemberId;
        this.finalReviewJson = finalReviewJson;
        this.reviewedAt = LocalDateTime.now();
    }

}
