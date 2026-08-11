package com.raota.ramenshop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;


@Embeddable
public record BusinessHours(
        @Column(name = "closed_days", length = 50)
        String closedDays,

        @Column(name = "open_time")
        LocalTime openTime,

        @Column(name = "close_time")
        LocalTime closeTime,

        @Column(name = "break_start")
        LocalTime breakStart,

        @Column(name = "break_end")
        LocalTime breakEnd,

        @Column(name = "parking_info", length = 100)
        String parkingInfo
) {
    public BusinessHours {
        // 심야 영업(다음 날 종료)을 허용하기 위해 종료 시간의 시작 시간 이후 여부 검증을 제거합니다.
    }

    public static BusinessHours of(
            String closedDays,
            LocalTime open,
            LocalTime close,
            LocalTime breakStart,
            LocalTime breakEnd,
            String parkingInfo
    ) {
        return new BusinessHours(closedDays, open, close, breakStart, breakEnd, parkingInfo);
    }

    public String toDisplayString() {
        return "%s %s~%s (Break %s~%s, Parking %s)".formatted(
                closedDays != null ? closedDays : "없음",
                openTime, closeTime, breakStart, breakEnd
                , parkingInfo != null ? parkingInfo : "없음"
        );
    }
}
