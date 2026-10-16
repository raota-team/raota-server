package com.raota.mobile.ramenlog.presentation.request;

import com.raota.mobile.ramenlog.application.command.MobileReportCommand;
import com.raota.mobile.ramenlog.domain.model.ReportReason;
import com.raota.mobile.ramenlog.domain.model.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MobileReportRequest(@NotNull ReportTargetType targetType,
        @NotNull @Pattern(regexp = "[0-9]{1,18}") String targetId, @NotNull ReportReason reason) {

    public MobileReportCommand toCommand() {
        return new MobileReportCommand(targetType, Long.valueOf(targetId), reason);
    }

}
