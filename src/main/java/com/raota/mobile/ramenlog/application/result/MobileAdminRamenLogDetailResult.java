package com.raota.mobile.ramenlog.application.result;

import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogResult.Author;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogResult.Shop;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 관리자 기록 상세다. 목록 항목의 모든 필드에 사진·맛 노트·재방문 의사·점수를 더한다. */
public record MobileAdminRamenLogDetailResult(
        String id,
        Author author,
        Shop shop,
        String menuName,
        String ramenType,
        LocalDate visitedAt,
        LogVisibility visibility,
        String note,
        String thumbnailUrl,
        int imageCount,
        int likeCount,
        int commentCount,
        int openReportCount,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<String> imageUrls,
        List<TasteNote> tasteNotes,
        RevisitIntention revisitIntention,
        Scores scores) {

    /** 저장된 순서대로다. 지금 정의에 없는 코드면 {@code category}와 {@code label}이 null이다. */
    public record TasteNote(String code, String category, String label) {}

    /** v1에서 옮긴 기록은 점수가 없어 {@code scores}가 null이다. */
    public record Scores(int satisfaction, int brothDensity, int noodleFirmness, int topping) {}
}
