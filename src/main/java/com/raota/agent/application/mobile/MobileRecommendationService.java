package com.raota.agent.application.mobile;

import com.raota.agent.application.mobile.command.MobileRecommendationCommand;
import com.raota.agent.application.mobile.port.MobileRecommendationReasonPort;
import com.raota.agent.application.mobile.result.MobileRecommendationResult;
import com.raota.agent.application.mobile.result.MobileRecommendationTicketResult;
import com.raota.agent.application.mobile.result.MobileTodayRecommendationsResult;
import com.raota.agent.application.ramenshop.port.RamenShopSearchDocumentPort;
import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.agent.domain.recommendation.model.ShownRecommendation;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.facade.MobileShopAgentFacade;
import com.raota.mobile.shop.application.facade.MobileShopAgentFacade.MobileShopReasonFacts;
import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 앱 AI 큐레이터 추천이다. 고른 국물은 필수 조건이라 그 라멘 종류의 v2 매장만 후보로 삼는다.
 *
 * <p>
 * 좌표가 오면 반경 없이 가장 가까운 {@value #NEAREST_CANDIDATES}곳, 없으면 조회수 순 최대
 * {@value #MAX_CANDIDATES}곳이 후보다. 지역이 오면 그 지역 매장만 남긴다. 후보를 상황·우선순위·자유 입력과 매장 문서(소개·리뷰)의
 * 벡터 유사도로 줄 세우고, 좌표가 있으면 가까운 순위만큼 최대 {@value #PROXIMITY_WEIGHT}점을 더한다. 문장이 없으면 벡터 검색 없이
 * 후보 순서(가까운 순·조회수 순) 그대로다.
 * </p>
 *
 * <p>
 * 고른 매장마다 LLM이 조건에 맞는 이유를 2~3문장으로 쓴다. 근거는 라멘 종류·태그·리뷰 요약·매장 설명·가격대·서비스·거리·영업 여부·기록 수다.
 * 이유는 덤이라 검색과 같은 시간 예산 안에서만 기다리고, 실패하거나 시간이 모자라면 이유 없이 매장만 돌려준다.
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

    /** 추천 이유를 기다리는 최대 시간이다. 매장 6곳의 이유를 2~3문장씩 쓰므로 넉넉히 두고, 남은 시간 예산이 더 짧으면 그만큼만 기다린다. */
    static final Duration REASON_TIMEOUT = Duration.ofSeconds(12);

    /** 남은 시간이 이보다 짧으면 LLM을 부르지 않는다. */
    static final long MIN_REASON_BUDGET_MS = 100;

    /** 프롬프트가 길어지지 않게 리뷰 요약·매장 설명은 각각 이만큼만 넘긴다. */
    static final int MAX_INTRODUCTION_LENGTH = 500;

    /** 모델이 규칙을 어기고 길게 써도 화면이 밀리지 않게 자른다. 프롬프트가 바라는 길이(200자 안팎)보다 조금 넉넉하다. */
    static final int MAX_REASON_LENGTH = 260;

    /**
     * 서버 한 대에서 동시에 도는 검색 수 상한이다. 시간 초과로 응답을 끊어도 외부 호출이 끝날 때까지 스레드를 차지한다. 대기열 없이 빈 스레드가
     * 없으면 바로 거절한다.
     */
    static final int MAX_CONCURRENT_SEARCHES = 8;

    private final RamenShopSearchDocumentPort documents;

    private final MobileShopAgentFacade shops;

    private final MobileRecommendationReasonPort reasonWriter;

    private final MobileRecommendationRateLimiter rateLimiter;

    private final MobileRecommendationLogger logger;

    private final MobileRecommendationTickets tickets;

    private final ObjectMapper mapper;

    private final Duration timeout;

    /** JDBC·HTTP 블로킹 호출이라 가상 스레드 대신 크기가 정해진 플랫폼 스레드를 쓴다. */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            MAX_CONCURRENT_SEARCHES,
            MAX_CONCURRENT_SEARCHES,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            Thread.ofPlatform().name("ai-recommendation-", 0).daemon(true).factory(),
            new ThreadPoolExecutor.AbortPolicy());

    public MobileRecommendationService(
            RamenShopSearchDocumentPort documents,
            MobileShopAgentFacade shops,
            MobileRecommendationReasonPort reasonWriter,
            MobileRecommendationRateLimiter rateLimiter,
            MobileRecommendationLogger logger,
            MobileRecommendationTickets tickets,
            ObjectMapper mapper,
            @Value("${app.mobile.recommendation.timeout:20s}") Duration timeout) {
        this.documents = documents;
        this.shops = shops;
        this.reasonWriter = reasonWriter;
        this.rateLimiter = rateLimiter;
        this.logger = logger;
        this.tickets = tickets;
        this.mapper = mapper;
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
        if (tickets.remaining(userId) == 0) {
            throw new MobileException(
                    MobileErrorCode.DAILY_LIMIT_EXCEEDED,
                    "오늘 식권 " + tickets.dailyLimit() + "장을 모두 뽑았어요. 내일 다시 뽑을 수 있어요.");
        }
        long startedAt = System.nanoTime();
        boolean located = command.latitude() != null;
        int limit = MAX_CANDIDATES;
        if (located) {
            limit = NEAREST_CANDIDATES;
        }
        List<Long> candidates = shops.recommendationCandidates(
                command.soupType(), region, command.latitude(), command.longitude(), limit);
        List<Scored> ranked = rank(candidates, similarities(command.similarityQuery(), candidates), located);
        List<MobileShopSummaryResult> picked = shops.recommendable(
                userId,
                ranked.stream().map(Scored::shopId).toList(),
                command.latitude(),
                command.longitude(),
                MAX_SHOPS);
        Map<String, String> reasonByShop = reasons(command, region, picked, startedAt);
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        String recommendationId =
                logger.record(userId, command, candidates.size(), shown(picked, ranked, reasonByShop), latencyMs);
        return new MobileRecommendationResult(
                recommendationId, picked, appliedConditions(command.preferenceLabels(), region, located), reasonByShop);
    }

    /** 오늘 뽑은 식권과 남은 장수다. 식권마다 그때 보여준 순서대로, 지금도 추천해도 되는 매장만 다시 요약한다. */
    public MobileTodayRecommendationsResult today(Long userId) {
        List<ShownRecommendation> shown = tickets.today(userId);
        return new MobileTodayRecommendationsResult(
                tickets.dailyLimit(), Math.max(0, tickets.dailyLimit() - shown.size()), toTickets(userId, shown));
    }

    /**
     * 오늘 것을 포함해 지금까지 뽑은 식권을 최신순으로 읽는다. 매장이 모두 걸러진 식권도 빼지 않고 빈 매장 목록으로 돌려준다. 빼면 뽑은 날짜와 장수가
     * 실제와 달라진다.
     */
    public CursorPage<MobileRecommendationTicketResult> history(Long userId, String cursor, int size) {
        CursorPage<ShownRecommendation> page = tickets.history(userId, cursor, size);
        return new CursorPage<>(toTickets(userId, page.items()), page.nextCursor(), page.hasNext());
    }

    /** 여러 식권의 매장을 한 번에 다시 요약한 뒤, 식권마다 그때 보여준 순서대로 지금도 추천해도 되는 매장만 최대 {@value #MAX_SHOPS}곳 남긴다. */
    private List<MobileRecommendationTicketResult> toTickets(Long userId, List<ShownRecommendation> shown) {
        List<Long> shopIds = shown.stream()
                .flatMap(run -> run.items().stream())
                .map(MobileRecommendationRun.Item::shopId)
                .distinct()
                .toList();
        Map<Long, MobileShopSummaryResult> recommendable = new HashMap<>();
        for (MobileShopSummaryResult shop : shops.recommendable(userId, shopIds, null, null, shopIds.size())) {
            recommendable.put(Long.valueOf(shop.id()), shop);
        }
        List<MobileRecommendationTicketResult> result = new ArrayList<>(shown.size());
        for (ShownRecommendation run : shown) {
            result.add(toTicket(run, recommendable));
        }
        return result;
    }

    private MobileRecommendationTicketResult toTicket(
            ShownRecommendation run, Map<Long, MobileShopSummaryResult> recommendable) {
        List<MobileShopSummaryResult> picked = new ArrayList<>();
        Map<String, String> reasons = new LinkedHashMap<>();
        for (MobileRecommendationRun.Item item : run.items()) {
            MobileShopSummaryResult shop = recommendable.get(item.shopId());
            if (shop != null && picked.size() < MAX_SHOPS) {
                picked.add(shop);
            }
            if (item.reason() != null) {
                reasons.put(String.valueOf(item.shopId()), item.reason());
            }
        }
        Map<String, String> conditions = mapper.readValue(run.conditionsJson(), new TypeReference<>() {});
        MobileRecommendationCommand asked = new MobileRecommendationCommand(
                conditions.get("soup"),
                conditions.get("mood"),
                conditions.get("priority"),
                conditions.get("text"),
                null,
                null,
                conditions.get("region"));
        return new MobileRecommendationTicketResult(
                String.valueOf(run.id()),
                run.createdAt(),
                picked,
                appliedConditions(asked.preferenceLabels(), trimToNull(asked.region()), run.located()),
                reasons);
    }

    /**
     * 고른 매장마다 추천 이유(2~3문장)다. 보여주는 매장의 이유만 남기고, LLM 장애·시간 초과·동시 실행 상한이면 빈 결과다.
     */
    private Map<String, String> reasons(
            MobileRecommendationCommand command, String region, List<MobileShopSummaryResult> picked, long startedAt) {
        if (picked.isEmpty()) {
            return Map.of();
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        long budgetMs = Math.min(REASON_TIMEOUT.toMillis(), timeout.toMillis() - elapsedMs);
        if (budgetMs < MIN_REASON_BUDGET_MS) {
            return Map.of();
        }
        String conditions = reasonConditions(command, region);
        String context = reasonContext(picked);
        Future<Map<String, String>> future;
        try {
            future = executor.submit(() -> reasonWriter.reasons(conditions, context));
        } catch (RejectedExecutionException exception) {
            log.warn("AI 추천 이유 동시 실행 상한 초과: limit={}", MAX_CONCURRENT_SEARCHES);
            return Map.of();
        }
        try {
            return shownReasons(future.get(budgetMs, TimeUnit.MILLISECONDS), picked);
        } catch (TimeoutException exception) {
            future.cancel(true);
            log.warn("AI 추천 이유 시간 초과: budgetMs={}", budgetMs);
        } catch (ExecutionException exception) {
            log.warn("AI 추천 이유 생성 실패", exception.getCause());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        return Map.of();
    }

    private static String reasonConditions(MobileRecommendationCommand command, String region) {
        StringBuilder conditions = new StringBuilder();
        appendLine(conditions, "국물", command.soup());
        appendLine(conditions, "상황", command.mood());
        appendLine(conditions, "중요한 점", command.priority());
        appendLine(conditions, "더 바라는 점", command.text());
        appendLine(conditions, "지역", region);
        if (command.latitude() != null) {
            appendLine(conditions, "위치", "가까운 곳");
        }
        return conditions.toString().trim();
    }

    private String reasonContext(List<MobileShopSummaryResult> picked) {
        Map<Long, MobileShopReasonFacts> facts = shops.reasonFacts(
                picked.stream().map(shop -> Long.valueOf(shop.id())).toList());
        StringBuilder context = new StringBuilder();
        for (MobileShopSummaryResult shop : picked) {
            context.append("- 매장 ID: ").append(shop.id()).append('\n');
            appendLine(context, "  라멘 종류", String.join(", ", shop.ramenTypes()));
            appendLine(context, "  태그", String.join(", ", shop.tags()));
            appendLine(context, "  한 줄 소개", shop.tagline());
            MobileShopReasonFacts fact = facts.get(Long.valueOf(shop.id()));
            if (fact != null) {
                appendLine(context, "  리뷰 요약", truncate(fact.aiReviewSummary(), MAX_INTRODUCTION_LENGTH));
                appendLine(context, "  매장 설명", truncate(fact.description(), MAX_INTRODUCTION_LENGTH));
                appendLine(context, "  가격대", priceRange(fact.priceMin(), fact.priceMax()));
                appendLine(context, "  서비스", String.join(", ", fact.perks()));
            }
            if (shop.distanceMeters() != null) {
                appendLine(context, "  거리", shop.distanceMeters() + "m");
            }
            if (Boolean.TRUE.equals(shop.isOpen())) {
                appendLine(context, "  영업", "지금 영업 중");
            }
            if (shop.logCount() > 0) {
                appendLine(context, "  라오타 기록", shop.logCount() + "개");
            }
        }
        return context.toString().trim();
    }

    private static String priceRange(Integer min, Integer max) {
        if (min == null && max == null) {
            return null;
        }
        if (min == null || max == null || min.equals(max)) {
            Integer price = min;
            if (price == null) {
                price = max;
            }
            return price + "원";
        }
        return min + "~" + max + "원";
    }

    /** 모델이 지어낸 ID나 빈 문장은 버리고, 한 줄로 다듬어 길이를 자른다. */
    private static Map<String, String> shownReasons(
            Map<String, String> generated, List<MobileShopSummaryResult> picked) {
        Map<String, String> shown = new LinkedHashMap<>();
        for (MobileShopSummaryResult shop : picked) {
            String reason = generated.get(shop.id());
            if (reason == null) {
                continue;
            }
            String oneLine = reason.replaceAll("\\s+", " ").trim();
            if (!oneLine.isEmpty()) {
                shown.put(shop.id(), truncate(oneLine, MAX_REASON_LENGTH));
            }
        }
        return shown;
    }

    private static void appendLine(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim() + "…";
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

    private static List<MobileRecommendationRun.Item> shown(
            List<MobileShopSummaryResult> picked, List<Scored> ranked, Map<String, String> reasons) {
        Map<Long, Double> scores = new HashMap<>();
        for (Scored scored : ranked) {
            scores.put(scored.shopId(), scored.score());
        }
        List<MobileRecommendationRun.Item> items = new ArrayList<>(picked.size());
        for (int index = 0; index < picked.size(); index++) {
            String shopId = picked.get(index).id();
            items.add(new MobileRecommendationRun.Item(
                    index + 1, Long.valueOf(shopId), scores.get(Long.valueOf(shopId)), reasons.get(shopId)));
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
        } catch (RejectedExecutionException exception) {
            log.warn("AI 추천 동시 검색 상한 초과: limit={}", MAX_CONCURRENT_SEARCHES);
            throw unavailable();
        }
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            log.warn("AI 추천 검색 시간 초과: timeout={}", timeout);
            throw unavailable();
        } catch (ExecutionException exception) {
            log.warn("AI 추천 검색 실패", exception.getCause());
            throw unavailable();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        }
    }

    private static List<String> appliedConditions(List<String> preferenceLabels, String region, boolean located) {
        List<String> conditions = new ArrayList<>(preferenceLabels);
        if (region != null) {
            conditions.add(region);
        }
        if (located) {
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

    private record Scored(Long shopId, double score, int order) {}
}
