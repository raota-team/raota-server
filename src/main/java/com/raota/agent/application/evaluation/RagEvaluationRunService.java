package com.raota.agent.application.evaluation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationCaseResultJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationCaseResultEntity;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import com.raota.global.presentation.common.PageResponse;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RagEvaluationRunService {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final RagEvaluationDatasetLoader datasetLoader;
    private final RagEvaluationRunJpaRepository runRepository;
    private final RagEvaluationCaseResultJpaRepository caseRepository;
    private final RagEvaluationRunner executionService;
    private final ObjectMapper objectMapper;
    private final String serverCommit;
    private final String appContractVersion;
    private final String vectorIndexVersion;
    private final String modelMetadata;

    public RagEvaluationRunService(
            RagEvaluationDatasetLoader datasetLoader,
            RagEvaluationRunJpaRepository runRepository,
            RagEvaluationCaseResultJpaRepository caseRepository,
            RagEvaluationRunner executionService,
            ObjectMapper objectMapper,
            @Value("${app.rag.evaluation.server-commit:unknown}") String serverCommit,
            @Value("${app.rag.evaluation.app-contract-version:v1}") String appContractVersion,
            @Value("${app.rag.evaluation.vector-index-version:unknown}") String vectorIndexVersion,
            @Value("${app.rag.evaluation.model-metadata:{}}") String modelMetadata
    ) {
        this.datasetLoader = datasetLoader;
        this.runRepository = runRepository;
        this.caseRepository = caseRepository;
        this.executionService = executionService;
        this.objectMapper = objectMapper;
        this.serverCommit = serverCommit;
        this.appContractVersion = appContractVersion;
        this.vectorIndexVersion = vectorIndexVersion;
        this.modelMetadata = modelMetadata;
    }

    public DatasetView dataset(String requestedVersion) {
        RagEvaluationDataset dataset = datasetLoader.loadDefault();
        if (requestedVersion != null && !requestedVersion.isBlank()
                && !dataset.version().equals(requestedVersion)) {
            throw new IllegalArgumentException("지원하지 않는 평가셋 버전입니다: " + requestedVersion);
        }
        return new DatasetView(
                dataset.version(),
                dataset.cases().size(),
                dataset.countsByType(),
                List.of(RagEvaluationSplit.DEV, RagEvaluationSplit.HOLDOUT)
        );
    }

    @Transactional
    public synchronized RunStart start(String datasetVersion, RagEvaluationSplit split, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key 헤더는 필수입니다.");
        }
        if (idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key는 128자 이하여야 합니다.");
        }

        RagEvaluationDataset dataset = datasetLoader.loadDefault();
        if (datasetVersion != null && !dataset.version().equals(datasetVersion)) {
            throw new IllegalArgumentException("지원하지 않는 평가셋 버전입니다: " + datasetVersion);
        }
        RagEvaluationSplit targetSplit = split == null ? RagEvaluationSplit.DEV : split;

        var existing = runRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            RagEvaluationRunEntity run = existing.get();
            if (run.getIdempotencyExpiresAt() == null || run.getIdempotencyExpiresAt().isAfter(LocalDateTime.now())) {
                if (!run.getDatasetVersion().equals(dataset.version()) || run.getSplit() != targetSplit) {
                    throw new IllegalArgumentException("같은 Idempotency-Key로 다른 평가 요청을 보낼 수 없습니다.");
                }
                return new RunStart(run.getRunId(), run.getStatus(), true);
            }
            throw new IllegalArgumentException("Idempotency-Key가 만료되었습니다. 새 키를 사용하세요.");
        }

        Collection<RagEvaluationStatus> activeStatuses = List.of(
                RagEvaluationStatus.QUEUED,
                RagEvaluationStatus.RUNNING
        );
        if (runRepository.findFirstByStatusInOrderByCreatedAtDesc(activeStatuses).isPresent()) {
            throw new IllegalStateException("이미 실행 중인 RAG 평가가 있습니다.");
        }

        String runId = UUID.randomUUID().toString();
        RagEvaluationRunEntity run = RagEvaluationRunEntity.queued(
                runId,
                dataset.version(),
                targetSplit,
                idempotencyKey,
                LocalDateTime.now().plusHours(24),
                serverCommit,
                appContractVersion,
                vectorIndexVersion,
                modelMetadata
        );
        runRepository.saveAndFlush(run);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executionService.execute(runId, dataset, targetSplit);
            }
        });
        return new RunStart(runId, RagEvaluationStatus.QUEUED, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<RunView> list(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RunView> result = runRepository.findAll(pageable).map(this::toRunView);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CursorPage<RunView> listCursor(String cursor, int size) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        List<RagEvaluationRunEntity> rows;
        if (cursor == null || cursor.isBlank()) {
            rows = runRepository.findAll(PageRequest.of(
                    0,
                    safeSize + 1,
                    Sort.by(Sort.Direction.DESC, "createdAt")
                            .and(Sort.by(Sort.Direction.DESC, "runId"))
            )).getContent();
        } else {
            CursorValue value = decodeCursor(cursor);
            rows = runRepository.findAfterCursor(
                    value.createdAt(),
                    value.runId(),
                    PageRequest.of(0, safeSize + 1)
            );
        }
        boolean hasNext = rows.size() > safeSize;
        List<RunView> items = rows.stream().limit(safeSize).map(this::toRunView).toList();
        String nextCursor = hasNext && !items.isEmpty() ? encodeCursor(items.getLast()) : null;
        return new CursorPage<>(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public RunView get(String runId) {
        return toRunView(findRun(runId));
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseView> cases(String runId, int page, int size, RagEvaluationCaseType type, RagEvaluationCaseStatus status) {
        findRun(runId);
        List<CaseView> filtered = caseRepository.findByRunIdOrderByIdAsc(runId).stream()
                .filter(item -> type == null || item.getCaseType() == type)
                .filter(item -> status == null || item.getStatus() == status)
                .map(this::toCaseView)
                .toList();
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        int from = Math.min(filtered.size(), safePage * safeSize);
        int to = Math.min(filtered.size(), from + safeSize);
        List<CaseView> items = filtered.subList(from, to);
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / safeSize);
        PageResponse.PageMeta meta = new PageResponse.PageMeta(
                safePage, safeSize, filtered.size(), totalPages, to < filtered.size(), safePage > 0
        );
        return new PageResponse<>(items, meta);
    }

    @Transactional
    public CaseView review(String runId, String caseId, Long reviewerMemberId, ReviewCommand command) {
        RagEvaluationRunEntity run = findRun(runId);
        if (run.getStatus() != RagEvaluationStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("검수 대기 상태인 실행만 사례를 검수할 수 있습니다.");
        }
        if (command == null || command.finalScore() == null || command.finalScore() < 0 || command.finalScore() > 2
                || command.approved() == null) {
            throw new IllegalArgumentException("최종 점수는 0~2 사이이고 승인 여부는 필수입니다.");
        }
        RagEvaluationCaseResultEntity entity = caseRepository.findByRunIdAndCaseId(runId, caseId)
                .orElseThrow(() -> new IllegalArgumentException("평가 사례를 찾을 수 없습니다: " + caseId));
        if (entity.getCaseType() == RagEvaluationCaseType.SEARCH) {
            throw new IllegalArgumentException("검색 사례는 사람 검수 대상이 아닙니다.");
        }
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("finalScore", command.finalScore());
        review.put("approved", command.approved());
        review.put("verdict", command.verdict());
        review.put("opinion", command.opinion());
        entity.recordReview(reviewerMemberId, toJson(review));
        return toCaseView(caseRepository.save(entity));
    }

    @Transactional
    public RunView finalize(String runId) {
        RagEvaluationRunEntity run = findRun(runId);
        if (run.getStatus() != RagEvaluationStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("검수 대기 상태인 실행만 확정할 수 있습니다.");
        }
        RagEvaluationDataset dataset = datasetLoader.loadDefault();
        boolean missingReview = dataset.casesFor(run.getSplit()).stream()
                .filter(this::isGenerated)
                .map(item -> caseRepository.findByRunIdAndCaseId(runId, item.caseId()).orElse(null))
                .anyMatch(item -> item == null || item.getFinalReviewJson() == null || item.getFinalReviewJson().isBlank());
        if (missingReview) {
            throw new IllegalStateException("생성 사례의 사람 검수가 모두 끝나야 기준선을 확정할 수 있습니다.");
        }
        run.markCompleted();
        return toRunView(runRepository.save(run));
    }

    @Transactional(readOnly = true)
    public ExportView export(String runId) {
        RagEvaluationRunEntity run = findRun(runId);
        return new ExportView(
                toRunView(run),
                caseRepository.findByRunIdOrderByIdAsc(runId).stream().map(this::toCaseView).toList()
        );
    }

    private boolean isGenerated(RagEvaluationCase evaluationCase) {
        return evaluationCase.type() != RagEvaluationCaseType.SEARCH && !evaluationCase.contractOnly();
    }

    private RagEvaluationRunEntity findRun(String runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("평가 실행을 찾을 수 없습니다: " + runId));
    }

    private RunView toRunView(RagEvaluationRunEntity run) {
        return new RunView(
                run.getRunId(), run.getDatasetVersion(), run.getSplit(), run.getStatus(),
                parseJson(run.getAggregateMetrics()), run.getServerCommit(), run.getAppContractVersion(),
                run.getVectorIndexVersion(), parseJson(run.getModelMetadata()), run.getFatalError(),
                run.getCreatedAt(), run.getStartedAt(), run.getCompletedAt()
        );
    }

    private CaseView toCaseView(RagEvaluationCaseResultEntity item) {
        return new CaseView(
                item.getRunId(), item.getCaseId(), item.getCaseType(), item.getStatus(),
                parseJson(item.getRequestJson()), parseJson(item.getExpectedJson()), parseJson(item.getResponseJson()),
                parseJson(item.getEvidenceJson()), parseJson(item.getMetricsJson()), parseJson(item.getAutoJudgementJson()),
                parseJson(item.getFinalReviewJson()), item.getLatencyMs(), item.getErrorType(), item.getErrorMessage(),
                item.getReviewerMemberId(), item.getReviewedAt()
        );
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            return objectMapper.getNodeFactory().textNode(value);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("JSON 변환에 실패했습니다.", exception);
        }
    }

    private String encodeCursor(RunView view) {
        if (view.createdAt() == null) {
            return null;
        }
        String raw = view.createdAt() + "|" + view.runId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private CursorValue decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = raw.indexOf('|');
            if (separator <= 0 || separator == raw.length() - 1) {
                throw new IllegalArgumentException("잘못된 cursor입니다.");
            }
            return new CursorValue(
                    LocalDateTime.parse(raw.substring(0, separator)),
                    raw.substring(separator + 1)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("잘못된 cursor입니다.", exception);
        }
    }

    public record DatasetView(String version, int totalCases, Map<RagEvaluationCaseType, Long> countsByType,
                              List<RagEvaluationSplit> availableSplits) {
    }

    public record RunStart(String runId, RagEvaluationStatus status, boolean idempotentReplay) {
    }

    public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {
    }

    private record CursorValue(LocalDateTime createdAt, String runId) {
    }

    public record RunView(String runId, String datasetVersion, RagEvaluationSplit split, RagEvaluationStatus status,
                          JsonNode aggregateMetrics, String serverCommit, String appContractVersion,
                          String vectorIndexVersion, JsonNode modelMetadata, String fatalError,
                          LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime completedAt) {
    }

    public record CaseView(String runId, String caseId, RagEvaluationCaseType caseType, RagEvaluationCaseStatus status,
                           JsonNode request, JsonNode expected, JsonNode response, JsonNode evidence, JsonNode metrics,
                           JsonNode autoJudgement, JsonNode finalReview, Long latencyMs, String errorType,
                           String errorMessage, Long reviewerMemberId, LocalDateTime reviewedAt) {
    }

    public record ReviewCommand(Integer finalScore, Boolean approved, String verdict, String opinion) {
    }

    public record ExportView(RunView run, List<CaseView> cases) {
    }
}
