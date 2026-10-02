package com.raota.mobile.shop.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

/** 확인된 요일별 시간만으로 서울의 현재 영업 여부를 판정한다. */
@Service
public class MobileShopOpenStatus {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final Clock clock;

    public MobileShopOpenStatus(Clock clock) {
        this.clock = clock;
    }

    public Boolean isOpen(Instant verifiedAt, List<Hours> hours) {
        if (verifiedAt == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), SEOUL);
        LocalDate today = now.toLocalDate();
        return within(hours, today, now) || within(hours, today.minusDays(1), now);
    }

    private boolean within(List<Hours> hours, LocalDate date, LocalDateTime now) {
        int day = date.getDayOfWeek().getValue();
        for (Hours hour : hours) {
            if (hour.dayOfWeek() != day || hour.closed() || hour.opensAt() == null || hour.closesAt() == null) {
                continue;
            }
            LocalDateTime opens = date.atTime(hour.opensAt());
            LocalDateTime closes = date.atTime(hour.closesAt());
            boolean overnight = !closes.isAfter(opens);
            if (overnight) {
                closes = closes.plusDays(1);
            }
            if (now.isBefore(opens) || !now.isBefore(closes)) {
                continue;
            }
            if (hour.breakStart() != null && hour.breakEnd() != null) {
                LocalDateTime breakStart = date.atTime(hour.breakStart());
                if (overnight && breakStart.isBefore(opens)) {
                    breakStart = breakStart.plusDays(1);
                }
                LocalDateTime breakEnd = date.atTime(hour.breakEnd());
                if (!breakEnd.isAfter(breakStart)) {
                    breakEnd = breakEnd.plusDays(1);
                }
                if (!now.isBefore(breakStart) && now.isBefore(breakEnd)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    /** 특정 ISO 요일의 영업·휴식 시간을 가진 순수 값이다. */
    public record Hours(int dayOfWeek, LocalTime opensAt, LocalTime closesAt, LocalTime breakStart, LocalTime breakEnd,
            boolean closed) {
    }

}
