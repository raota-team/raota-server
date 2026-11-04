package com.raota.mobile.ramenlog.application.result;

import com.raota.mobile.ramenlog.domain.model.TasteAxisAverages;
import com.raota.mobile.ramenlog.domain.model.TasteReportKind;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record MobileTasteReportResult(
        String id,
        TasteReportKind kind,
        LocalDate cutoffDate,
        Instant createdAt,
        Instant seenAt,
        int bowlCount,
        int shopCount,
        Map<String, Integer> typeCounts,
        TasteAxisAverages axisAverages,
        List<TopShop> topShops,
        List<String> recentLogIds) {

    public record TopShop(
            Shop shop, int visitCount, String topMenu, String topRamenType, LocalDate lastVisitedAt, String photoUrl) {}

    public record Shop(String id, String name, String branchName) {}
}
