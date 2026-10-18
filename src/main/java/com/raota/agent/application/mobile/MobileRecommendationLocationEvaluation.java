package com.raota.agent.application.mobile;

import com.raota.agent.application.evaluation.RagEvaluationCase;
import com.raota.agent.application.evaluation.RagEvaluationCaseType;
import com.raota.agent.application.evaluation.RagEvaluationDataset;
import com.raota.agent.application.evaluation.RagEvaluationDatasetLoader;
import com.raota.agent.application.evaluation.RagEvaluationSplit;
import com.raota.agent.application.evaluation.RagExpectedShop;
import com.raota.agent.application.ramenshop.search.AiRamenShopSearchQueryParser;
import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockClient.LockToken;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * 일회용: v2 추천의 위치 hard filter 적용 전후를 운영 RAG 색인으로 비교한다(#46). 결과를 남긴 뒤 이 클래스와 관리자 API는 지운다.
 *
 * <p>
 * {@code rag-mobile-v1.1} DEV 검색 사례마다 같은 질문을 지역 없이 한 번, 질문에서 뽑은 지역으로 한 번 추천해 1등
 * 적중(hit@1)과 상위 6곳 재현율(recall@6)을 비교한다. 결과는 Redis에 14일 보관한다.
 * </p>
 */
@Slf4j
@Service
public class MobileRecommendationLocationEvaluation {

    static final String RESULT_KEY = "v2:recommendation-eval:location:latest";

    static final String LOCK_KEY = "lock:v2:recommendation-eval:location";

    private static final String DATASET_VERSION = "rag-mobile-v1.1";

    private final MobileRecommendationService recommendations;

    private final AiRamenShopSearchQueryParser parser;

    private final RagEvaluationDatasetLoader datasets;

    private final RedisLockClient locks;

    private final StringRedisTemplate redis;

    private final ObjectMapper mapper;

    private final Clock clock;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MobileRecommendationLocationEvaluation(MobileRecommendationService recommendations,
            AiRamenShopSearchQueryParser parser, RagEvaluationDatasetLoader datasets, RedisLockClient locks,
            StringRedisTemplate redis, ObjectMapper mapper, Clock clock) {
        this.recommendations = recommendations;
        this.parser = parser;
        this.datasets = datasets;
        this.locks = locks;
        this.redis = redis;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** 백그라운드에서 평가를 시작한다. 이미 실행 중이면 거절한다. */
    public void start() {
        LockToken lock = locks.tryAcquire(LOCK_KEY, Duration.ofMinutes(30))
            .orElseThrow(() -> new IllegalStateException("위치 필터 평가가 이미 실행 중입니다."));
        executor.submit(() -> {
            try {
                Report report = run();
                redis.opsForValue().set(RESULT_KEY, mapper.writeValueAsString(report), Duration.ofDays(14));
                log.info("위치 필터 평가 완료: cases={}, hit1={}→{}, recall6={}→{}", report.cases().size(), report.baseHitAt1(),
                        report.locatedHitAt1(), report.baseRecallAt6(), report.locatedRecallAt6());
            }
            catch (RuntimeException exception) {
                log.warn("위치 필터 평가 실패", exception);
            }
            finally {
                locks.release(lock);
            }
        });
    }

    /** 마지막 평가 결과다. 아직 없으면 비어 있다. */
    public Optional<Report> latest() {
        String json = redis.opsForValue().get(RESULT_KEY);
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(mapper.readValue(json, Report.class));
    }

    Report run() {
        Instant startedAt = clock.instant();
        RagEvaluationDataset dataset = datasets.load(DATASET_VERSION);
        List<CaseResult> results = new ArrayList<>();
        for (RagEvaluationCase evaluationCase : dataset.cases()) {
            if (evaluationCase.split() == RagEvaluationSplit.DEV
                    && evaluationCase.type() == RagEvaluationCaseType.SEARCH) {
                results.add(evaluate(evaluationCase));
            }
        }
        return new Report(DATASET_VERSION, startedAt, clock.instant(), average(results, CaseResult::baseHitAt1),
                average(results, CaseResult::locatedHitAt1), average(results, CaseResult::baseRecallAt6),
                average(results, CaseResult::locatedRecallAt6), results);
    }

    private CaseResult evaluate(RagEvaluationCase evaluationCase) {
        String query = evaluationCase.request().path("query").asString();
        Set<Long> relevant = evaluationCase.relevantShops()
            .stream()
            .map(RagExpectedShop::shopId)
            .collect(Collectors.toSet());
        List<String> regions = parser.parse(query).regions();
        String region = null;
        if (!regions.isEmpty()) {
            region = regions.getFirst();
        }
        try {
            List<Long> base = recommendations.rankForEvaluation(query, null);
            List<Long> located = base;
            if (region != null) {
                located = recommendations.rankForEvaluation(query, region);
            }
            return new CaseResult(evaluationCase.caseId(), query, region, relevant.stream().sorted().toList(), base,
                    located, hitAt1(base, relevant), hitAt1(located, relevant), recallAt6(base, relevant),
                    recallAt6(located, relevant), null);
        }
        catch (RuntimeException exception) {
            return new CaseResult(evaluationCase.caseId(), query, region, relevant.stream().sorted().toList(),
                    List.of(), List.of(), 0, 0, 0, 0, exception.getMessage());
        }
    }

    private static double hitAt1(List<Long> shown, Set<Long> relevant) {
        if (!shown.isEmpty() && relevant.contains(shown.getFirst())) {
            return 1;
        }
        return 0;
    }

    private static double recallAt6(List<Long> shown, Set<Long> relevant) {
        if (relevant.isEmpty()) {
            return 0;
        }
        return shown.stream().filter(relevant::contains).count() / (double) relevant.size();
    }

    private static double average(List<CaseResult> results, ToDoubleFunction<CaseResult> metric) {
        return results.stream().mapToDouble(metric).average().orElse(0);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    /** 사례별 결과와 평균이다. 위치가 없는 사례는 두 결과가 같다. */
    public record Report(String datasetVersion, Instant startedAt, Instant finishedAt, double baseHitAt1,
            double locatedHitAt1, double baseRecallAt6, double locatedRecallAt6, List<CaseResult> cases) {
    }

    /** {@code error}가 있으면 그 사례는 검색에 실패해 0점으로 셌다. */
    public record CaseResult(String caseId, String query, String region, List<Long> relevantShopIds,
            List<Long> baseShopIds, List<Long> locatedShopIds, double baseHitAt1, double locatedHitAt1,
            double baseRecallAt6, double locatedRecallAt6, String error) {
    }

}
