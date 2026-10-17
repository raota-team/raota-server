package com.raota.agent.application.mobile;

import com.raota.agent.application.recommendation.ReviewSummaryService;
import com.raota.agent.application.recommendation.query.ReviewSummaryQuery;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse.SummaryDetail;
import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockClient.LockToken;
import com.raota.mobile.shop.application.facade.MobileShopAgentFacade;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 기존 리뷰 요약(외부 리뷰 청크 근거 + LLM)으로 매장 AI 요약을 만들어 v2 매장에 저장한다. 외부 리뷰 원문은 저장하지 않는다.
 *
 * <p>
 * 요약은 v1 매장 정보와 RAG 색인을 쓴다(매장 ID는 v1·v2가 같다). 근거 리뷰가 없으면 저장하지 않는다. 전체 생성은 매장마다 LLM을 부르므로
 * 요약이 없는 공개 매장을 한 번에 최대 {@value #MAX_BULK_SHOPS}곳까지만 백그라운드에서 하나씩 처리한다. 매장당 수 초가 걸려도 락
 * TTL(2시간) 안에 끝나도록 크기를 제한하고, 남은 매장은 다시 실행해 이어서 처리한다. 서버가 2대여도 한 번만 돌도록 Redis 락을 잡는다.
 * </p>
 */
@Slf4j
@Service
public class MobileShopAiSummaryService {

    static final String BULK_LOCK_KEY = "lock:v2:shop-ai-summary";

    private static final Duration BULK_LOCK_TTL = Duration.ofHours(2);

    static final int MAX_BULK_SHOPS = 100;

    private static final String FOCUS = "전반적인 맛과 메뉴";

    private final ReviewSummaryService summaries;

    private final MobileShopAgentFacade shops;

    private final RedisLockClient locks;

    private final Clock clock;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MobileShopAiSummaryService(ReviewSummaryService summaries, MobileShopAgentFacade shops,
            RedisLockClient locks, Clock clock) {
        this.summaries = summaries;
        this.shops = shops;
        this.locks = locks;
        this.clock = clock;
    }

    public Result summarize(Long shopId) {
        ReviewSummaryResponse response;
        try {
            response = summaries.summarizeReviews(new ReviewSummaryQuery(shopId, FOCUS));
        }
        catch (EntityNotFoundException exception) {
            return new Result(shopId, Outcome.NO_SOURCE_SHOP);
        }
        if (response.reviewCount() == null || response.reviewCount() == 0) {
            return new Result(shopId, Outcome.NO_REVIEWS);
        }
        List<String> lines = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        add(lines, keywords, "장점", response.summary().pros());
        add(lines, keywords, "아쉬운 점", response.summary().cons());
        add(lines, keywords, "추천 메뉴", response.summary().recommendedMenu());
        if (lines.isEmpty()) {
            return new Result(shopId, Outcome.NO_REVIEWS);
        }
        boolean saved = shops.saveAiSummary(shopId, String.join("\n", lines), keywords,
                Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        if (!saved) {
            return new Result(shopId, Outcome.NO_V2_SHOP);
        }
        return new Result(shopId, Outcome.SAVED);
    }

    /** 요약이 없는 공개 매장을 최대 100곳 백그라운드에서 처리하고 대상·남은 수를 바로 돌려준다. 이미 실행 중이면 거절한다. */
    public BulkStart summarizeMissing() {
        LockToken lock = locks.tryAcquire(BULK_LOCK_KEY, BULK_LOCK_TTL)
            .orElseThrow(() -> new IllegalStateException("매장 AI 요약 전체 생성이 이미 실행 중입니다."));
        List<Long> missing;
        try {
            missing = shops.publishedShopIdsWithoutAiSummary();
        }
        catch (RuntimeException exception) {
            locks.release(lock);
            throw exception;
        }
        List<Long> batch = missing.subList(0, Math.min(missing.size(), MAX_BULK_SHOPS));
        executor.submit(() -> runBulk(List.copyOf(batch), lock));
        return new BulkStart(batch.size(), missing.size() - batch.size());
    }

    private void runBulk(List<Long> shopIds, LockToken lock) {
        int saved = 0;
        int skipped = 0;
        int failed = 0;
        try {
            for (Long shopId : shopIds) {
                try {
                    if (summarize(shopId).outcome() == Outcome.SAVED) {
                        saved++;
                    }
                    else {
                        skipped++;
                    }
                }
                catch (RuntimeException exception) {
                    failed++;
                    log.warn("매장 AI 요약 생성 실패: shopId={}", shopId, exception);
                }
            }
            log.info("매장 AI 요약 전체 생성 완료: total={}, saved={}, skipped={}, failed={}", shopIds.size(), saved, skipped,
                    failed);
        }
        finally {
            locks.release(lock);
        }
    }

    /** 리뷰가 부족할 때 넣는 대체 문구(제목이 "~부족")는 저장하지 않는다. */
    private static void add(List<String> lines, List<String> keywords, String label, SummaryDetail detail) {
        if (detail == null || detail.title() == null || detail.title().endsWith("부족")) {
            return;
        }
        lines.add(label + ": " + detail.body());
        keywords.add(detail.title());
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public enum Outcome {

        SAVED, NO_REVIEWS, NO_SOURCE_SHOP, NO_V2_SHOP

    }

    public record Result(Long shopId, Outcome outcome) {
    }

    /** 이번에 처리할 매장 수와 다음 실행에 남은 매장 수다. */
    public record BulkStart(int queued, int remaining) {
    }

}
