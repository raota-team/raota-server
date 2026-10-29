package com.raota.mobile.ramenlog.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 새 기록의 내용이다. 모든 항목이 필요하다. 부분 수정({@link MobileRamenLogEdit})은 null을 "그대로 둔다"로 읽으므로 같은 타입을 쓰지
 * 않는다.
 */
public record MobileRamenLogDraft(
        LocalDate visitedAt,
        String menuName,
        String ramenType,
        MobileRamenLogScores scores,
        RevisitIntention revisitIntention,
        String note,
        List<String> tasteNoteCodes,
        LogVisibility visibility) {}
