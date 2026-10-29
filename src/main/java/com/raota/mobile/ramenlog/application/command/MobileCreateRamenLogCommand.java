package com.raota.mobile.ramenlog.application.command;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogDraft;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogScores;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import java.time.LocalDate;
import java.util.List;

public record MobileCreateRamenLogCommand(
        String shopId,
        LocalDate visitedAt,
        String menuName,
        String ramenType,
        MobileRamenLogScores scores,
        RevisitIntention revisitIntention,
        String note,
        List<String> tasteNoteCodes,
        LogVisibility visibility,
        List<String> imageUrls) {

    /** 기록 테이블에 들어가는 내용만 꺼낸다. 매장 ID 변환과 사진 저장은 서비스가 맡는다. */
    public MobileRamenLogDraft toDraft() {
        return new MobileRamenLogDraft(
                visitedAt, menuName, ramenType, scores, revisitIntention, note, tasteNoteCodes, visibility);
    }
}
