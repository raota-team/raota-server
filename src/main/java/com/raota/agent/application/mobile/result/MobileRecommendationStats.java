package com.raota.agent.application.mobile.result;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 "AI 추천 성과"다. 기간은 한국 날짜(양 끝 포함)이고, 찜·기록은 추천 뒤 {@code attributionDays}일 안에 같은 회원이 그
 * 매장에 남긴 것만 센다.
 */
public record MobileRecommendationStats(
        LocalDate from,
        LocalDate to,
        int attributionDays,
        Row overall,
        List<Row> byAlgorithmVersion,
        List<Row> bySoup,
        List<Row> byRank) {

    /**
     * {@code runs}는 매장을 보여준 추천 수, {@code impressions}는 보여준 매장 칸 수다.
     * {@code opened}·{@code bookmarked}·{@code logged}는 칸 기준, {@code topOpened}는 1위 매장을 연
     * 추천 수다.
     */
    public record Row(String key, int runs, int impressions, int opened, int topOpened, int bookmarked, int logged) {}
}
