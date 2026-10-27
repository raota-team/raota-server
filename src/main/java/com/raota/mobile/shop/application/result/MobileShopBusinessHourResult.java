package com.raota.mobile.shop.application.result;

import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** ISO 요일과 HH:mm 형태의 시계 시간을 내보낸다. */
public record MobileShopBusinessHourResult(
        int dayOfWeek,
        String opensAt,
        String closesAt,
        String breakStart,
        String breakEnd,
        String lastOrderAt,
        boolean isClosed) {

    private static final DateTimeFormatter CLOCK_TIME = DateTimeFormatter.ofPattern("HH:mm");

    public static MobileShopBusinessHourResult from(MobileShopBusinessHour hour) {
        return new MobileShopBusinessHourResult(
                hour.getDayOfWeek(),
                clockTime(hour.getOpensAt()),
                clockTime(hour.getClosesAt()),
                clockTime(hour.getBreakStart()),
                clockTime(hour.getBreakEnd()),
                clockTime(hour.getLastOrderAt()),
                hour.isClosed());
    }

    /** 앱·관리자 응답이 같이 쓰는 HH:mm 형식이다. 값이 없으면 {@code null}이다. */
    public static String clockTime(LocalTime value) {
        if (value == null) {
            return null;
        }
        return value.format(CLOCK_TIME);
    }
}
