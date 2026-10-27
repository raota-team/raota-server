package com.raota.mobile.ramenlog.application.result;

import com.raota.mobile.ramenlog.domain.model.ReportReason;
import com.raota.mobile.ramenlog.domain.model.ReportStatus;
import com.raota.mobile.ramenlog.domain.model.ReportTargetType;
import java.time.Instant;

/** 관리자 신고 목록의 한 건과 대상 미리보기다. */
public record MobileAdminReport(
        String id,
        ReportStatus status,
        ReportReason reason,
        Person reporter,
        Instant createdAt,
        Instant resolvedAt,
        int openReportCount,
        Target target) {

    /** 탈퇴로 지워진 회원이면 닉네임이 null이다. */
    public record Person(String id, String nickname) {}

    /**
     * {@code text}는 기록이면 메모, 댓글이면 내용이다. 댓글이면 {@code menuName}과 {@code imageUrl}이 null이다.
     */
    public record Target(
            ReportTargetType type,
            String id,
            String ramenLogId,
            Person author,
            String text,
            String menuName,
            String imageUrl,
            boolean deleted) {}
}
