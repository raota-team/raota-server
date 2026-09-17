package com.raota.agent.infrastructure.persistence.evaluation.entity;

import com.raota.agent.application.evaluation.RagEvaluationSplit;
import com.raota.agent.application.evaluation.RagEvaluationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_rag_evaluation_run")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagEvaluationRunEntity {

    public static final String ACTIVE_SLOT = "ACTIVE";

    @Id
    @Column(name = "run_id", length = 36, nullable = false)
    private String runId;

    @Column(name = "dataset_version", length = 64, nullable = false)
    private String datasetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_name", length = 20, nullable = false)
    private RagEvaluationSplit split;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private RagEvaluationStatus status;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "idempotency_expires_at")
    private LocalDateTime idempotencyExpiresAt;

    @Column(name = "server_commit", length = 64)
    private String serverCommit;

    @Column(name = "app_contract_version", length = 64)
    private String appContractVersion;

    @Column(name = "vector_index_version", length = 128)
    private String vectorIndexVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_metadata", columnDefinition = "json")
    private String modelMetadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aggregate_metrics", columnDefinition = "json")
    private String aggregateMetrics;

    @Column(name = "fatal_error", columnDefinition = "text")
    private String fatalError;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "heartbeat_at")
    private LocalDateTime heartbeatAt;

    @Column(name = "active_slot", length = 16)
    private String activeSlot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RagEvaluationRunEntity(
            String runId,
            String datasetVersion,
            RagEvaluationSplit split,
            String idempotencyKey,
            LocalDateTime idempotencyExpiresAt,
            String serverCommit,
            String appContractVersion,
            String vectorIndexVersion,
            String modelMetadata
    ) {
        this.runId = runId;
        this.datasetVersion = datasetVersion;
        this.split = split;
        this.status = RagEvaluationStatus.QUEUED;
        this.idempotencyKey = idempotencyKey;
        this.idempotencyExpiresAt = idempotencyExpiresAt;
        this.serverCommit = serverCommit;
        this.appContractVersion = appContractVersion;
        this.vectorIndexVersion = vectorIndexVersion;
        this.modelMetadata = modelMetadata;
        this.activeSlot = ACTIVE_SLOT;
        this.heartbeatAt = LocalDateTime.now();
    }

    public static RagEvaluationRunEntity queued(
            String runId,
            String datasetVersion,
            RagEvaluationSplit split,
            String idempotencyKey,
            LocalDateTime idempotencyExpiresAt,
            String serverCommit,
            String appContractVersion,
            String vectorIndexVersion,
            String modelMetadata
    ) {
        return new RagEvaluationRunEntity(
                runId,
                datasetVersion,
                split,
                idempotencyKey,
                idempotencyExpiresAt,
                serverCommit,
                appContractVersion,
                vectorIndexVersion,
                modelMetadata
        );
    }

    public void markCompleted() {
        this.status = RagEvaluationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
