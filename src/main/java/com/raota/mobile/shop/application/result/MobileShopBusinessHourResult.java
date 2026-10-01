package com.raota.mobile.shop.application.result;

/** ISO 요일과 HH:mm 형태의 시계 시간을 내보낸다. */
public record MobileShopBusinessHourResult(int dayOfWeek, String opensAt, String closesAt, String breakStart,
        String breakEnd, String lastOrderAt, boolean isClosed) {
}
