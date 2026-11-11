package com.raota.mobile.ramenlog.application.result;

import com.raota.mobile.ramenlog.domain.model.TasteReportKind;
import java.time.LocalDate;

public record MobileDrawnTasteReportResult(String id, TasteReportKind kind, LocalDate cutoffDate) {}
