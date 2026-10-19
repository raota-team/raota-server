package com.raota.mobile.shop.application.command;

import java.time.LocalTime;

/** ISO 요일 1(월)~7(일) 하루의 영업시간이다. 마감이 시작보다 이르면 다음 날 마감이다. */
public record MobileAdminBusinessHourCommand(int dayOfWeek, boolean closed, LocalTime opensAt, LocalTime closesAt,
        LocalTime breakStart, LocalTime breakEnd, LocalTime lastOrderAt) {
}
