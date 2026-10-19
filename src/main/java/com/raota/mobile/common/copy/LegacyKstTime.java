package com.raota.mobile.common.copy;

import java.time.LocalDateTime;

/** v1 {@code DATETIME}(한국 시간으로 저장)을 v2 {@code DATETIME}(UTC로 저장) 값으로 바꾼다. */
public final class LegacyKstTime {

    /**
     * V13 마이그레이션이 기존 회원의 {@code created_at}을 DB 기본값({@code CURRENT_TIMESTAMP(6)}, 운영 DB는
     * UTC)으로 채운 시각이다. 이 값은 이미 UTC이므로 9시간을 빼지 않는다.
     */
    static final LocalDateTime V13_BACKFILLED_UTC = LocalDateTime.parse("2026-06-12T01:21:21.695633");

    private static final long KST_OFFSET_HOURS = 9;

    private LegacyKstTime() {
    }

    public static LocalDateTime toUtc(LocalDateTime kst) {
        if (kst == null || kst.equals(V13_BACKFILLED_UTC)) {
            return kst;
        }
        return kst.minusHours(KST_OFFSET_HOURS);
    }

}
