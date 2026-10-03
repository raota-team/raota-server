package com.raota.mobile.ramenlog.domain.model;

import java.time.LocalDate;
import java.util.List;

/** 기록 부분 수정 내용이다. null인 항목은 바꾸지 않는다. */
public record MobileRamenLogEdit(LocalDate visitedAt, String menuName, String ramenType, MobileRamenLogScores scores,
        RevisitIntention revisitIntention, String note, List<String> tasteNoteCodes, LogVisibility visibility) {
}
