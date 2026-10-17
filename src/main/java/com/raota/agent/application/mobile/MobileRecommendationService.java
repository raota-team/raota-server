package com.raota.agent.application.mobile;

import com.raota.agent.application.mobile.command.MobileRecommendationCommand;
import com.raota.agent.application.mobile.result.MobileRecommendationResult;
import com.raota.agent.application.ramenshop.port.RamenShopSearchDocumentPort;
import com.raota.agent.application.ramenshop.query.ParsedAiRamenShopSearchQuery;
import com.raota.agent.application.ramenshop.result.AiRamenShopSearchHit;
import com.raota.agent.application.ramenshop.search.AiRamenShopSearchQueryParser;
import com.raota.agent.application.ramenshop.search.AiRamenShopSearchReranker;
import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.facade.MobileShopAgentFacade;
import com.raota.mobile.shop.application.result.MobileShopSummary;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 앱 AI 큐레이터의 조건을 기존 검색 문장으로 바꿔 기존 RAG(쿼리 파서 → 벡터 검색 → 리랭커)로 매장을 고르고, 같은 ID의 v2 매장으로 다시
 * 확인해 돌려준다.
 *
 * <p>
 * 위치 조건(지역, 좌표)은 리랭커 가산점이 아니라 v2 매장 기준의 hard filter로 쓴다. 걸러낸 뒤에도 6곳을 채우도록 위치 조건이 있으면 벡터
 * 후보를 더 많이 읽는다.
 * </p>
 */
@Slf4j
@Service
public class MobileRecommendationService {

    static final int MAX_SHOPS = 6;

    static final int RADIUS_METERS = 3_000;

    private static final int CANDIDATE_DOCUMENTS = 30;

    private static final int LOCATION_CANDIDATE_DOCUMENTS = 100;

    private static final double SIMILARITY_THRESHOLD = 0.35;

    /**
     * 서버 한 대에서 동시에 도는 검색 수 상한이다. 시간 초과로 응답을 끊어도 외부 호출이 끝날 때까지 스레드를 차지한다. 대기열 없이 빈 스레드가
     * 없으면 바로 거절한다.
     */
    static final int MAX_CONCURRENT_SEARCHES = 8;

    private final AiRamenShopSearchQueryParser parser;

    private final RamenShopSearchDocumentPort documents;

    private final AiRamenShopSearchReranker reranker;

    private final MobileShopAgentFacade shops;

    private final MobileRecommendationRateLimiter rateLimiter;

    private final MobileRecommendationLogger logger;

    private final Duration timeout;

    /** JDBC·HTTP 블로킹 호출이라 가상 스레드 대신 크기가 정해진 플랫폼 스레드를 쓴다. */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(MAX_CONCURRENT_SEARCHES, MAX_CONCURRENT_SEARCHES,
            60, TimeUnit.SECONDS, new SynchronousQueue<>(),
            Thread.ofPlatform().name("ai-recommendation-", 0).daemon(true).factory(),
            new ThreadPoolExecutor.AbortPolicy());

    public MobileRecommendationService(AiRamenShopSearchQueryParser parser, RamenShopSearchDocumentPort documents,
            AiRamenShopSearchReranker reranker, MobileShopAgentFacade shops,
            MobileRecommendationRateLimiter rateLimiter, MobileRecommendationLogger logger,
            @Value("${app.mobile.recommendation.timeout:10s}") Duration timeout) {
        this.parser = parser;
        this.documents = documents;
        this.reranker = reranker;
        this.shops = shops;
        this.rateLimiter = rateLimiter;
        this.logger = logger;
        this.timeout = timeout;
    }

    public MobileRecommendationResult recommend(Long userId, MobileRecommendationCommand command) {
        String query = command.query();
        if (query.isEmpty()) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "추천 조건을 하나 이상 골라 주세요.");
        }
        if ((command.latitude() == null) != (command.longitude() == null)) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "위도와 경도는 함께 보내 주세요.");
        }
        String region = trimToNull(command.region());
        rateLimiter.acquire(userId);
        long startedAt = System.nanoTime();
        boolean located = region != null || command.latitude() != null;
        List<AiRamenShopSearchHit> hits = search(query, located);
        List<MobileShopSummary> picked = shops.recommendable(userId,
                hits.stream().map(AiRamenShopSearchHit::shopId).toList(), region, command.latitude(),
                command.longitude(), RADIUS_METERS, MAX_SHOPS);
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        String recommendationId = logger.record(userId, command, hits.size(), shown(picked, hits), latencyMs);
        return new MobileRecommendationResult(recommendationId, picked, appliedConditions(command, region));
    }

    private List<MobileRecommendationRun.Item> shown(List<MobileShopSummary> picked, List<AiRamenShopSearchHit> hits) {
        Map<Long, Double> scores = new HashMap<>();
        for (AiRamenShopSearchHit hit : hits) {
            scores.putIfAbsent(hit.shopId(), hit.finalScore());
        }
        List<MobileRecommendationRun.Item> items = new ArrayList<>(picked.size());
        for (int index = 0; index < picked.size(); index++) {
            Long shopId = Long.valueOf(picked.get(index).id());
            items.add(new MobileRecommendationRun.Item(index + 1, shopId, scores.get(shopId)));
        }
        return items;
    }

    /**
     * 검색 장애와 시간 초과는 503으로 알린다. 앱은 이때 인기·거리순 목록으로 대신 보여준다. 동시 검색이 상한에 닿아도 기다리지 않고 바로
     * 503이다.
     */
    private List<AiRamenShopSearchHit> search(String query, boolean located) {
        int topK = CANDIDATE_DOCUMENTS;
        if (located) {
            topK = LOCATION_CANDIDATE_DOCUMENTS;
        }
        int candidates = topK;
        Future<List<AiRamenShopSearchHit>> future;
        try {
            future = executor.submit(() -> {
                ParsedAiRamenShopSearchQuery parsed = parser.parse(query);
                return reranker.rerank(
                        documents.searchShopDocuments(parsed.expandedQuery(), candidates, SIMILARITY_THRESHOLD), parsed,
                        candidates);
            });
        }
        catch (RejectedExecutionException exception) {
            log.warn("AI 추천 동시 검색 상한 초과: limit={}", MAX_CONCURRENT_SEARCHES);
            throw unavailable();
        }
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException exception) {
            future.cancel(true);
            log.warn("AI 추천 검색 시간 초과: timeout={}", timeout);
            throw unavailable();
        }
        catch (ExecutionException exception) {
            log.warn("AI 추천 검색 실패", exception.getCause());
            throw unavailable();
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        }
    }

    private List<String> appliedConditions(MobileRecommendationCommand command, String region) {
        List<String> conditions = new ArrayList<>(command.preferenceLabels());
        if (region != null) {
            conditions.add(region);
        }
        if (command.latitude() != null) {
            conditions.add(RADIUS_METERS / 1000 + "km 이내");
        }
        return conditions;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static MobileException unavailable() {
        return new MobileException(MobileErrorCode.RECOMMENDATION_UNAVAILABLE, "지금은 추천을 만들 수 없습니다.");
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

}
