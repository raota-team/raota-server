package com.raota.mobile.ramenlog.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record MobileTasteReportFigures(
        int bowlCount,
        int shopCount,
        Map<String, Integer> typeCounts,
        TasteAxisAverages axisAverages,
        List<TasteTopShop> topShops,
        List<Long> recentLogIds) {

    static final int TOP_SHOP_LIMIT = 3;

    static final int RECENT_LOG_LIMIT = 5;

    /** 방문일이 늦은 순, 같은 날이면 ID가 큰 순이다. */
    private static final Comparator<MobileRamenLog> LATEST_FIRST = Comparator.comparing(MobileRamenLog::getVisitedAt)
            .thenComparing(MobileRamenLog::getId)
            .reversed();

    public static MobileTasteReportFigures of(List<MobileRamenLog> logs) {
        List<MobileRamenLog> latestFirst = logs.stream().sorted(LATEST_FIRST).toList();
        Set<Long> shops = new HashSet<>();
        for (MobileRamenLog log : logs) {
            shops.add(log.getShopId());
        }
        return new MobileTasteReportFigures(
                logs.size(),
                shops.size(),
                ranked(latestFirst.stream().map(MobileRamenLog::getRamenType).toList()),
                averages(logs),
                topShops(latestFirst),
                latestFirst.stream()
                        .limit(RECENT_LOG_LIMIT)
                        .map(MobileRamenLog::getId)
                        .toList());
    }

    private static TasteAxisAverages averages(List<MobileRamenLog> logs) {
        int scored = 0;
        double satisfaction = 0;
        double brothDensity = 0;
        double noodleFirmness = 0;
        double topping = 0;
        double revisit = 0;
        for (MobileRamenLog log : logs) {
            if (log.getSatisfactionScore() == null) {
                continue;
            }
            scored++;
            satisfaction += log.getSatisfactionScore();
            brothDensity += log.getBrothDensityScore();
            noodleFirmness += log.getNoodleFirmnessScore();
            topping += log.getToppingScore();
            revisit += log.getRevisitIntention().score();
        }
        if (scored == 0) {
            return new TasteAxisAverages(0, null, null, null, null, null);
        }
        return new TasteAxisAverages(
                scored,
                satisfaction / scored,
                brothDensity / scored,
                noodleFirmness / scored,
                topping / scored,
                revisit / scored);
    }

    private static List<TasteTopShop> topShops(List<MobileRamenLog> latestFirst) {
        Map<Long, List<MobileRamenLog>> byShop = new LinkedHashMap<>();
        for (MobileRamenLog log : latestFirst) {
            byShop.computeIfAbsent(log.getShopId(), id -> new ArrayList<>()).add(log);
        }
        List<TasteTopShop> shops = new ArrayList<>(byShop.size());
        for (Map.Entry<Long, List<MobileRamenLog>> entry : byShop.entrySet()) {
            List<MobileRamenLog> visits = entry.getValue();
            shops.add(new TasteTopShop(
                    entry.getKey(),
                    visits.size(),
                    mostFrequent(
                            visits.stream().map(MobileRamenLog::getMenuName).toList()),
                    mostFrequent(
                            visits.stream().map(MobileRamenLog::getRamenType).toList()),
                    visits.getFirst().getVisitedAt()));
        }
        return shops.stream()
                .sorted(Comparator.comparingInt(TasteTopShop::visitCount)
                        .reversed()
                        .thenComparing(TasteTopShop::lastVisitedAt, Comparator.<LocalDate>reverseOrder())
                        .thenComparing(TasteTopShop::shopId))
                .limit(TOP_SHOP_LIMIT)
                .toList();
    }

    /** 가장 많이 나온 값. 같으면 먼저 나온(최신순 목록에서 더 최근인) 값이다. */
    private static String mostFrequent(List<String> latestFirst) {
        return ranked(latestFirst).keySet().iterator().next();
    }

    /** 많은 순이다. 안정 정렬이라 같으면 먼저 나온 값이 앞선다. */
    private static Map<String, Integer> ranked(List<String> values) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String value : values) {
            counts.merge(value, 1, Integer::sum);
        }
        Map<String, Integer> ranked = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> ranked.put(entry.getKey(), entry.getValue()));
        return ranked;
    }
}
