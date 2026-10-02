package com.raota.mobile.shop.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 서울의 현재 시각에서 영업시간의 경계와 확인 여부를 판정한다. */
class MobileShopOpenStatusTest {

    private static final Instant VERIFIED = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void 일반_영업과_브레이크_타임과_휴무를_구분한다() {
        var monday = new MobileShopOpenStatus.Hours(1, LocalTime.of(10, 0), LocalTime.of(20, 0), LocalTime.of(12, 0),
                LocalTime.of(13, 0), false);
        assertThat(at("2026-09-21T01:30:00Z").isOpen(VERIFIED, List.of(monday))).isTrue();
        assertThat(at("2026-09-21T03:30:00Z").isOpen(VERIFIED, List.of(monday))).isFalse();
        assertThat(at("2026-09-21T11:00:00Z").isOpen(VERIFIED, List.of(monday))).isFalse();
        var closed = new MobileShopOpenStatus.Hours(1, LocalTime.of(10, 0), LocalTime.of(20, 0), null, null, true);
        assertThat(at("2026-09-21T01:30:00Z").isOpen(VERIFIED, List.of(closed))).isFalse();
    }

    @Test
    void 자정_전후에는_전날_영업과_당일_영업을_각각_확인한다() {
        var monday = new MobileShopOpenStatus.Hours(1, LocalTime.of(22, 0), LocalTime.of(2, 0), LocalTime.of(23, 45),
                LocalTime.of(0, 15), false);
        var tuesday = new MobileShopOpenStatus.Hours(2, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, true);
        List<MobileShopOpenStatus.Hours> hours = List.of(monday, tuesday);
        assertThat(at("2026-09-21T14:30:00Z").isOpen(VERIFIED, hours)).isTrue();
        assertThat(at("2026-09-21T15:00:00Z").isOpen(VERIFIED, hours)).isFalse();
        assertThat(at("2026-09-21T16:00:00Z").isOpen(VERIFIED, hours)).isTrue();
        assertThat(at("2026-09-21T17:00:00Z").isOpen(VERIFIED, hours)).isFalse();
    }

    @Test
    void 검증되지_않은_시간은_계산하지_않는다() {
        var hours = List
            .of(new MobileShopOpenStatus.Hours(1, LocalTime.of(10, 0), LocalTime.of(20, 0), null, null, false));
        assertThat(at("2026-09-21T01:30:00Z").isOpen(null, hours)).isNull();
    }

    private MobileShopOpenStatus at(String instant) {
        return new MobileShopOpenStatus(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

}
