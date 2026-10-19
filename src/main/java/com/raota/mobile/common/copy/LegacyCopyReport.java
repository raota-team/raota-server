package com.raota.mobile.common.copy;

import java.util.Arrays;
import java.util.List;

/** 복사 한 단계의 테이블별 대조표다. */
public record LegacyCopyReport(List<LegacyCopyTable> tables) {

    public static LegacyCopyReport of(LegacyCopyTally... tallies) {
        return new LegacyCopyReport(Arrays.stream(tallies).map(LegacyCopyTally::result).toList());
    }

}
