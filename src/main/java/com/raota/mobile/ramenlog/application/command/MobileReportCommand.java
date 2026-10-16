package com.raota.mobile.ramenlog.application.command;

import com.raota.mobile.ramenlog.domain.model.ReportReason;
import com.raota.mobile.ramenlog.domain.model.ReportTargetType;

/** 신고할 기록이나 댓글과 신고 사유다. */
public record MobileReportCommand(ReportTargetType targetType, Long targetId, ReportReason reason) {
}
