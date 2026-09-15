package com.raota.agent.application.evaluation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationCaseResultJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationCaseResultEntity;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RagEvaluationExecutionService {
    private static final Duration CASE_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 2;

    private final RagEvaluationRunJpaRepository runRepository;
    private final RagEvaluationCaseResultJpaRepository caseRepository;
    private final RagEvaluationCaseExecutor caseExecutor;
    private final RagEvaluationJudge judge;
    private final RamenShopRepository ramenShopRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public RagEvaluationExecutionService(
            RagEvaluationRunJpaRepository runRepository,
            RagEvaluationCaseResultJpaRepository caseRepository,
            RagEvaluationCaseExecutor caseExecutor,
            RagEvaluationJudge judge,
            RamenShopRepository ramenShopRepository,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.runRepository = runRepository;
        this.caseRepository = caseRepository;
        this.caseExecutor = caseExecutor;
        this.judge = judge;
        this.ramenShopRepository = ramenShopRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Async("ragEvaluationTaskExecutor")
    public void execute(String runId, RagEvaluationDataset dataset, RagEvaluationSplit split) {
        try {
            transactionTemplate.executeWithoutResult(status -> runRepository.findById(runId)
                    .orElseThrow(() -> new IllegalArgumentException("평가 실행을 찾을 수 없습니다: " + runId))
                    .markRunning());

            List<Map<String, Double>> metricRows = new ArrayList<>();
            for (RagEvaluationCase evaluationCase : dataset.casesFor(split)) {
                persistCaseStarted(runId, evaluationCase);
                RagExecutionResult executionResult = executeWithTimeoutAndRetry(evaluationCase);
                Map<String, Double> metrics = calculateMetrics(evaluationCase, executionResult);
                RagEvaluationJudgeResult judgeResult = isGenerated(evaluationCase)
                        ? judge.suggest(evaluationCase, executionResult)
                        : null;

                persistCaseResult(runId, evaluationCase, executionResult, metrics, judgeResult);
                if (isGenerated(evaluationCase) && executionResult.status() == RagEvaluationCaseStatus.COMPLETED
                        && metrics.getOrDefault("schemaValid", 1.0) < 1.0) {
                    throw new RagEvaluationSafetyException("생성 응답 스키마 검증에 실패했습니다: " + evaluationCase.caseId());
                }
                if (evaluationCase.type() == RagEvaluationCaseType.SEARCH) {
                    ensurePublishedShops(executionResult.returnedShopIds());
                }
                if (!evaluationCase.contractOnly()) {
                    metricRows.add(metrics);
                }
            }

            Map<String, Object> aggregate = new LinkedHashMap<>();
            aggregate.put("caseCount", dataset.casesFor(split).size());
            aggregate.put("completedCaseCount", caseRepository.countByRunIdAndStatus(runId, RagEvaluationCaseStatus.COMPLETED));
            aggregate.put("errorCaseCount", caseRepository.countByRunIdAndStatus(runId, RagEvaluationCaseStatus.ERROR));
            aggregate.put("skippedCaseCount", caseRepository.countByRunIdAndStatus(runId, RagEvaluationCaseStatus.SKIPPED));
            aggregate.put("averageLatencyMs", averageLatency(runId));
            aggregate.put("metrics", RagEvaluationMetricCalculator.average(metricRows));
            String aggregateJson = toJson(aggregate);
            transactionTemplate.executeWithoutResult(status -> runRepository.findById(runId)
                    .orElseThrow(() -> new IllegalArgumentException("평가 실행을 찾을 수 없습니다: " + runId))
                    .markReviewRequired(aggregateJson));
        } catch (Exception exception) {
            transactionTemplate.executeWithoutResult(status -> runRepository.findById(runId)
                    .ifPresent(run -> run.markFailed(errorMessage(exception))));
        }
    }

    private void persistCaseStarted(String runId, RagEvaluationCase evaluationCase) {
        transactionTemplate.executeWithoutResult(status -> {
            RagEvaluationCaseResultEntity entity = caseRepository.findByRunIdAndCaseId(runId, evaluationCase.caseId())
                    .orElseGet(() -> RagEvaluationCaseResultEntity.pending(
                            runId,
                            evaluationCase.caseId(),
                            evaluationCase.type(),
                            toJson(evaluationCase.request()),
                            toJson(expectedJson(evaluationCase))
                    ));
            entity.markRunning();
            caseRepository.save(entity);
        });
    }

    private void persistCaseResult(
            String runId,
            RagEvaluationCase evaluationCase,
            RagExecutionResult result,
            Map<String, Double> metrics,
            RagEvaluationJudgeResult judgeResult
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            RagEvaluationCaseResultEntity entity = caseRepository.findByRunIdAndCaseId(runId, evaluationCase.caseId())
                    .orElseThrow(() -> new IllegalStateException("평가 사례 저장 행을 찾을 수 없습니다."));
            entity.recordExecution(
                    result.status(),
                    toJson(result.response()),
                    toJson(result.evidence()),
                    toJson(metrics),
                    toJson(judgeResult),
                    result.latencyMs(),
                    result.errorType(),
                    result.errorMessage()
            );
            caseRepository.save(entity);
        });
    }

    private RagExecutionResult executeWithTimeoutAndRetry(RagEvaluationCase evaluationCase) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            RagExecutionResult result = executeWithTimeout(evaluationCase);
            if (attempt == MAX_ATTEMPTS || !isRetryable(result)) {
                return result;
            }
        }
        throw new IllegalStateException("평가 사례 재시도 상태가 잘못되었습니다.");
    }

    private RagExecutionResult executeWithTimeout(RagEvaluationCase evaluationCase) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<RagExecutionResult> future = executor.submit(() -> caseExecutor.execute(evaluationCase));
        try {
            return future.get(CASE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            return RagExecutionResult.error(CASE_TIMEOUT.toMillis(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return RagExecutionResult.error(0, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            return RagExecutionResult.error(0, cause);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean isRetryable(RagExecutionResult result) {
        if (result == null || result.status() != RagEvaluationCaseStatus.ERROR) {
            return false;
        }
        String text = (result.errorType() + " " + result.errorMessage()).toLowerCase();
        return text.contains("timeout") || text.contains("429") || text.contains("5xx")
                || text.contains("500") || text.contains("502") || text.contains("503") || text.contains("504")
                || text.contains("toomanyrequests") || text.contains("badgateway")
                || text.contains("serviceunavailable") || text.contains("gatewaytimeout");
    }

    private Map<String, Double> calculateMetrics(RagEvaluationCase evaluationCase, RagExecutionResult result) {
        if (evaluationCase.type() == RagEvaluationCaseType.SEARCH) {
            return RagEvaluationMetricCalculator.calculateSearch(
                    result.returnedShopIds(),
                    evaluationCase.relevantShops(),
                    evaluationCase.expectsEmpty(),
                    evaluationCase.primaryK(),
                    evaluationCase.diagnosticK()
            );
        }
        return RagEvaluationMetricCalculator.calculateGeneration(
                evaluationCase,
                result.response(),
                result.fallback()
        );
    }

    private boolean isGenerated(RagEvaluationCase evaluationCase) {
        return evaluationCase.type() != RagEvaluationCaseType.SEARCH
                && !evaluationCase.contractOnly();
    }

    private void ensurePublishedShops(List<Long> shopIds) {
        List<Long> unsafeIds = shopIds.stream()
                .filter(id -> id == null || ramenShopRepository.findByIdAndPublishedTrue(id).isEmpty())
                .distinct()
                .toList();
        if (!unsafeIds.isEmpty()) {
            throw new RagEvaluationSafetyException("비공개 또는 존재하지 않는 매장이 검색 결과에 포함되었습니다: " + unsafeIds);
        }
    }

    private Map<String, Object> expectedJson(RagEvaluationCase evaluationCase) {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("caseType", evaluationCase.type());
        expected.put("split", evaluationCase.split());
        expected.put("authMode", evaluationCase.authMode());
        expected.put("relevantShops", evaluationCase.relevantShops());
        expected.put("expectsEmpty", evaluationCase.expectsEmpty());
        expected.put("requiredFacts", evaluationCase.requiredFacts());
        expected.put("forbiddenClaims", evaluationCase.forbiddenClaims());
        expected.put("expectsFallback", evaluationCase.expectsFallback());
        expected.put("contractOnly", evaluationCase.contractOnly());
        expected.put("primaryK", evaluationCase.primaryK());
        expected.put("diagnosticK", evaluationCase.diagnosticK());
        return expected;
    }

    private double averageLatency(String runId) {
        return caseRepository.findByRunIdOrderByIdAsc(runId).stream()
                .map(RagEvaluationCaseResultEntity::getLatencyMs)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            return "{}";
        }
    }

    private String errorMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "평가 실행에 치명적 오류가 발생했습니다.";
        }
        return exception.getMessage();
    }
}
