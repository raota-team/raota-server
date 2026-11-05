package com.raota.mobile.ramenlog.application.result;

import java.time.LocalDate;

/** {@code report}는 뽑은 적이 없으면 null이다. {@code upcoming.date}는 다음 달 1일이고, 첫 리포트 전이면 null이다. */
public record MobileCurrentTasteReportResult(MobileTasteReportResult report, Upcoming upcoming) {

    public record Upcoming(LocalDate date, int needed) {}
}
