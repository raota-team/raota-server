package com.raota.mobile.ramenlog.presentation.admin.request;

import com.raota.mobile.ramenlog.application.command.MobileReportResolution;
import jakarta.validation.constraints.NotNull;

public record MobileResolveReportRequest(@NotNull MobileReportResolution action) {
}
