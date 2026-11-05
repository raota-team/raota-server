package com.raota.mobile.ramenlog.application.result;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** {@code drawnTasteReport}는 이 기록으로 첫 리포트가 뽑혔을 때만 있고, 같은 멱등키의 재요청이면 null이다. */
public record MobileCreatedRamenLogResult(
        @JsonUnwrapped MobileRamenLogDetailResult log, MobileDrawnTasteReportResult drawnTasteReport) {}
