package com.raota.agent.application.mobile;

import com.raota.agent.application.mobile.command.MobileRecommendationCommand;
import com.raota.agent.application.mobile.result.MobileRecommendationResult;
import com.raota.agent.application.ramenshop.port.RamenShopSearchDocumentPort;
import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.facade.MobileShopAgentFacade;
import com.raota.mobile.shop.application.result.MobileShopSummary;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
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
 * 앱 AI 큐레이터 추천이다. 고른 국물은 필수 조건이라 그 라멘 종류의 v2 매장만 후보로 삼는다.
 *
 * <p>
 * 좌표가 오면 반경 없이 가장 가까운 {@value #NEAREST_CANDIDATES}곳, 없으면 조회수 순 최대
 * {@value #MAX_CANDIDATES}곳이 후보다. 지역이 오면 그 지역 매장만 남긴다. 후보를 상황·우선순위·자유 입력과 매장 문서(소개·리뷰)의
 * 벡터 유사도로 줄 세우고, 좌표가 있으면 가까운 순위만큼 최대 {@value #PROXIMITY_WEIGHT}점을 더한다. 문장이 없으면 벡터 검색 없이
 * 후보 순서(가까운 순·조회수 순) 그대로다.
 * </p>
 */
@Slf4j
@Service
public class MobileRecommendationService {

    static final int MAX_SHOPS = 6;

    static final int NEAREST_CANDIDATES = 10;

    static final int MAX_CANDIDATES = 200;

    /** 유사도는 매장끼리 0.1 안팎으로 갈려 거리가 순위를 뒤집을 수 있되 유사도를 덮지 않는 크기로 둔다. */
    static final double PROXIMITY_WEIGHT = 0.1;

    /**
     * 서버 한 대에서 동시에 도는 검색 수 상한이다. 시간 초과로 응답을 끊어도 외부 호출이 끝날 때까지 스레드를 차지한다. 대기열 없이 빈 스레드가
     * 없으면 바로 거절한다.
     */
    static final int MAX_CONCURRENT_SEARCHES = 8;

    private final RamenShopSearchDocumentPort documents;

    private final MobileShopAgentFacade shops;

    private final MobileRecommendationRateLimiter rateLimiter;

    private final MobileRecommendationLogger logger;

    private final Duration timeout;

    /** JDBC·HTTP 블로킹 호출이라 가상 스레드 대신 크기가 정해진 플랫폼 스레드를 쓴다. */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(MAX_CONCURRENT_SEARCHES, MAX_CONCURRENT_SEARCHES,
            60, TimeUnit.SECONDS, new SynchronousQueue<>(),
            Thread.ofPlatform().name("ai-recommendation-", 0).daemon(true).factory(),
            new ThreadPoolExecutor.AbortPolicy());

    public MobileRecommendationService(RamenShopSearchDocumentPort documents, MobileShopAgentFacade shops,
            MobileRecommendationRateLimiter rateLimiter, MobileRecommendationLogger logger,
            @Value("${app.mobile.recommendation.timeout:20s}") Duration timeout) {
        this.documents = documents;
        this.shops = shops;
        this.rateLimiter = rateLimiter;
        this.logger = logger;
        this.timeout = timeout;
    }

    public MobileRecommendationResult recommend(Long userId, MobileRecommendationCommand command) {
        if (!command.hasCondition()) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "추천 조건을 하나 이상 골라 주세요.");
        }
        if ((command.latitude() == null) != (command.longitude() == null)) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "위도와 경도는 함께 보내 주세요.");
        }
        String region = trimToNull(command.region());
        rateLimiter.acquire(userId);
        long startedAt = System.nanoTime();
        boolean located = command.latitude() != null;
        int limit = MAX_CANDIDATES;
        if (located) {
            limit = NEAREST_CANDIDATES;
        }
        List<Long> candidates = shops.recommendationCandidates(command.soupType(), region, command.latitude(),
                command.longitude(), limit);
        List<Scored> ranked = rank(candidates, similarities(command.similarityQuery(), candidates), located);
        List<MobileShopSummary> picked = shops.recommendable(userId, ranked.stream().map(Scored::shopId).toList(),
                command.latitude(), command.longitude(), MAX_SHOPS);
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        String recommendationId = logger.record(userId, command, candidates.size(), shown(picked, ranked), latencyMs);
        return new MobileRecommendationResult(recommendationId, picked, appliedConditions(command, region));
    }

    /** 후보 순서(가까운 순·조회수 순)를 동점 기준으로 두고 점수가 높은 순으로 세운다. */
    private static List<Scored> rank(List<Long> candidates, Map<Long, Double> similarities, boolean located) {
        List<Scored> scored = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            Long shopId = candidates.get(index);
            double score = similarities.getOrDefault(shopId, 0.0);
            if (located) {
                score += PROXIMITY_WEIGHT * (candidates.size() - index) / candidates.size();
            }
            scored.add(new Scored(shopId, score, index));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed().thenComparingInt(Scored::order));
        return scored;
    }

    private static List<MobileRecommendationRun.Item> shown(List<MobileShopSummary> picked, List<Scored> ranked) {
        Map<Long, Double> scores = new HashMap<>();
        for (Scored scored : ranked) {
            scores.put(scored.shopId(), scored.score());
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
    private Map<Long, Double> similarities(String query, List<Long> candidates) {
        if (query.isEmpty() || candidates.isEmpty()) {
            return Map.of();
        }
        Future<Map<Long, Double>> future;
        try {
            future = executor.submit(() -> documents.shopSimilarities(query, candidates));
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

    private static List<String> appliedConditions(MobileRecommendationCommand command, String region) {
        List<String> conditions = new ArrayList<>(command.preferenceLabels());
        if (region != null) {
            conditions.add(region);
        }
        if (command.latitude() != null) {
            conditions.add("가까운 순");
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

    private record Scored(Long shopId, double score, int order) {
    }

}
