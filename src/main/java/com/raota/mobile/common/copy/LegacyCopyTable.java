package com.raota.mobile.common.copy;

import java.util.Map;

/** v2 테이블 하나의 복사 대조 결과다. {@code source = copied + alreadyCopied + sum(excluded)}. */
public record LegacyCopyTable(String table, int source, int copied, int alreadyCopied,
        Map<LegacyCopyExclusion, Integer> excluded) {
}
