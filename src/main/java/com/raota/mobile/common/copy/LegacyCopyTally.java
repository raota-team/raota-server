package com.raota.mobile.common.copy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** 원본 행마다 정확히 한 번 분류해 대조표 등식이 항상 성립하게 센다. */
public final class LegacyCopyTally {

    private final String table;

    private final Map<LegacyCopyExclusion, Integer> excluded = new EnumMap<>(LegacyCopyExclusion.class);

    private int copied;

    private int alreadyCopied;

    public LegacyCopyTally(String table) {
        this.table = table;
    }

    public void copied() {
        copied++;
    }

    public void alreadyCopied() {
        alreadyCopied++;
    }

    public void exclude(LegacyCopyExclusion reason) {
        excluded.merge(reason, 1, Integer::sum);
    }

    public LegacyCopyTable result() {
        int excludedTotal = excluded.values().stream().mapToInt(Integer::intValue).sum();
        return new LegacyCopyTable(table, copied + alreadyCopied + excludedTotal, copied, alreadyCopied,
                Collections.unmodifiableMap(new EnumMap<>(excluded)));
    }

}
