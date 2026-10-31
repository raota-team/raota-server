package com.raota.mobile.ramenlog.application.result;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 관리자 기록 목록의 한 건이다. 삭제된 기록은 나오지 않는다. 비공개 기록은 ID·공개 범위·작성일·작성자만 채우고 나머지는 null이다.
 */
public record MobileAdminRamenLogResult(
        String id,
        LogVisibility visibility,
        Instant createdAt,
        Author author,
        Shop shop,
        String menuName,
        String ramenType,
        LocalDate visitedAt,
        String note,
        String thumbnailUrl,
        Integer imageCount,
        Integer likeCount,
        Integer commentCount,
        Integer openReportCount,
        Instant updatedAt) {

    /** 탈퇴로 지워진 회원이면 닉네임이 null이다. */
    public record Author(String id, String nickname) {}

    /** 매장 행이 없으면 이름과 지점명이 null이다. */
    public record Shop(String id, String name, String branchName) {}
}
