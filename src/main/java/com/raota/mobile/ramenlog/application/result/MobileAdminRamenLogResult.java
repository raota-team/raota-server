package com.raota.mobile.ramenlog.application.result;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import java.time.Instant;
import java.time.LocalDate;

/** 관리자 기록 목록의 한 건이다. 비공개·삭제된 기록도 포함한다. */
public record MobileAdminRamenLogResult(
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
        Instant deletedAt) {

    /** 탈퇴로 지워진 회원이면 닉네임이 null이다. */
    public record Author(String id, String nickname) {}

    /** 매장 행이 없으면 이름과 지점명이 null이다. */
    public record Shop(String id, String name, String branchName) {}
}
