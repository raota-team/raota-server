package com.raota.agent.application.mobile;

import com.raota.agent.application.mobile.result.MobileRecommendationStatsResult;
import com.raota.agent.application.mobile.result.MobileRecommendationStatsResult.Row;
import com.raota.agent.domain.recommendation.model.RecommendationImpression;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.mobile.ramenlog.application.facade.MobileRamenLogAgentFacade;
import com.raota.mobile.shop.application.facade.MobileShopAgentFacade;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 추천 노출 기록에 "추천 결과에서 열었는지"와, 같은 회원이 추천 뒤 7일 안에 그 매장을 찜·기록했는지를 붙여 센다. 찜·기록은 각 모듈의 facade로
 * 생성 시각만 읽는다. 해제한 찜은 행이 없어 빠지고, 같은 기간에 다른 경로로 한 찜·기록도 추천 덕으로 세므로 순수 효과보다 조금 높게 나온다.
 */
@Service
@RequiredArgsConstructor
public class MobileRecommendationStatsService {

    static final int ATTRIBUTION_DAYS = 7;

    static final int DEFAULT_DAYS = 30;

    static final int MAX_DAYS = 92;

    static final String OVERALL = "전체";

    static final String NO_SOUP = "(국물 없음)";

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static final Duration ATTRIBUTION = Duration.ofDays(ATTRIBUTION_DAYS);

    private final MobileRecommendationRunRepository runs;

    private final MobileShopAgentFacade shops;

    private final MobileRamenLogAgentFacade logs;

    private final ObjectMapper mapper;

    private final Clock clock;

    /** {@code from}·{@code to}가 없으면 오늘(KST)까지 최근 30일이다. 거꾸로이거나 92일을 넘으면 거절한다. */
    public MobileRecommendationStatsResult stats(LocalDate from, LocalDate to) {
        LocalDate end = to;
        if (end == null) {
            end = LocalDate.now(clock.withZone(SEOUL));
        }
        LocalDate start = from;
        if (start == null) {
            start = end.minusDays(DEFAULT_DAYS - 1L);
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new IllegalArgumentException("기간은 최대 " + MAX_DAYS + "일까지 볼 수 있습니다.");
        }
        Instant fromInstant = start.atStartOfDay(SEOUL).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(SEOUL).toInstant();
        List<RecommendationImpression> impressions = runs.findImpressions(fromInstant, toInstant);
        Set<Long> userIds = new HashSet<>();
        impressions.forEach(impression -> userIds.add(impression.userId()));
        Instant activityEnd = toInstant.plus(ATTRIBUTION);
        Map<String, List<Instant>> bookmarks =
                byUserShop(shops.bookmarksCreatedBetween(userIds, fromInstant, activityEnd).stream()
                        .map(bookmark -> new Activity(bookmark.userId(), bookmark.shopId(), bookmark.createdAt()))
                        .toList());
        Map<String, List<Instant>> written =
                byUserShop(logs.logsCreatedBetween(userIds, fromInstant, activityEnd).stream()
                        .map(log -> new Activity(log.userId(), log.shopId(), log.createdAt()))
                        .toList());

        Tally overall = new Tally(OVERALL);
        Map<String, Tally> byVersion = new LinkedHashMap<>();
        Map<String, Tally> bySoup = new LinkedHashMap<>();
        Map<Integer, Tally> byRank = new HashMap<>();
        for (RecommendationImpression impression : impressions) {
            String key = userShop(impression.userId(), impression.shopId());
            boolean bookmarked = within(bookmarks.get(key), impression.shownAt());
            boolean logged = within(written.get(key), impression.shownAt());
            overall.add(impression, bookmarked, logged);
            byVersion.computeIfAbsent(impression.algorithmVersion(), Tally::new).add(impression, bookmarked, logged);
            bySoup.computeIfAbsent(soup(impression.conditionsJson()), Tally::new)
                    .add(impression, bookmarked, logged);
            byRank.computeIfAbsent(impression.rank(), rank -> new Tally(String.valueOf(rank)))
                    .add(impression, bookmarked, logged);
        }
        List<Row> ranks = byRank.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().row())
                .toList();
        return new MobileRecommendationStatsResult(
                start, end, ATTRIBUTION_DAYS, overall.row(), byRuns(byVersion), byRuns(bySoup), ranks);
    }

    private static List<Row> byRuns(Map<String, Tally> tallies) {
        return tallies.values().stream()
                .map(Tally::row)
                .sorted(Comparator.comparingInt(Row::runs).reversed().thenComparing(Row::key))
                .toList();
    }

    private static Map<String, List<Instant>> byUserShop(List<Activity> activities) {
        Map<String, List<Instant>> result = new HashMap<>();
        for (Activity activity : activities) {
            result.computeIfAbsent(userShop(activity.userId(), activity.shopId()), key -> new ArrayList<>())
                    .add(activity.createdAt());
        }
        return result;
    }

    /** 추천을 보여준 때부터 7일 안에 한 행동이 있는지다. */
    private static boolean within(List<Instant> times, Instant shownAt) {
        if (times == null) {
            return false;
        }
        Instant until = shownAt.plus(ATTRIBUTION);
        return times.stream().anyMatch(time -> !time.isBefore(shownAt) && time.isBefore(until));
    }

    private String soup(String conditionsJson) {
        Map<String, String> conditions = mapper.readValue(conditionsJson, new TypeReference<Map<String, String>>() {});
        String soup = conditions.get("soup");
        if (soup == null || soup.isBlank()) {
            return NO_SOUP;
        }
        return soup.trim();
    }

    private static String userShop(Long userId, Long shopId) {
        return userId + ":" + shopId;
    }

    private record Activity(Long userId, Long shopId, Instant createdAt) {}

    /** 한 묶음의 건수다. 추천 수는 서로 다른 추천 ID로 센다. */
    private static final class Tally {

        private final String key;

        private final Set<Long> runIds = new HashSet<>();

        private int impressions;

        private int opened;

        private int topOpened;

        private int bookmarked;

        private int logged;

        Tally(String key) {
            this.key = key;
        }

        void add(RecommendationImpression impression, boolean wasBookmarked, boolean wasLogged) {
            runIds.add(impression.runId());
            impressions++;
            if (impression.openedAt() != null) {
                opened++;
                if (impression.rank() == 1) {
                    topOpened++;
                }
            }
            if (wasBookmarked) {
                bookmarked++;
            }
            if (wasLogged) {
                logged++;
            }
        }

        Row row() {
            return new Row(key, runIds.size(), impressions, opened, topOpened, bookmarked, logged);
        }
    }
}
