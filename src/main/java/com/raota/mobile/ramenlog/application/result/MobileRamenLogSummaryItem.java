package com.raota.mobile.ramenlog.application.result;

import java.time.LocalDate;

public record MobileRamenLogSummaryItem(String id, LocalDate visitedAt, Shop shop, String menuName, String ramenType,
        MobileRamenLogDetail.Scores scores) {

    public record Shop(String id, String name, String branchName) {
    }

}
