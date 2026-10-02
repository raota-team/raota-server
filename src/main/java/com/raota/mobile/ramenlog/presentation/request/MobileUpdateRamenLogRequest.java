package com.raota.mobile.ramenlog.presentation.request;

import com.raota.mobile.ramenlog.application.command.MobileUpdateRamenLogCommand;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

public record MobileUpdateRamenLogRequest(LocalDate visitedAt,
        @Size(max = 150) @Pattern(regexp = "(?s).*\\S.*") String menuName, String ramenType,
        @Valid MobileCreateRamenLogRequest.Scores scores, RevisitIntention revisitIntention,
        @Size(max = 500) String note, List<@NotBlank String> tasteNoteCodes, LogVisibility visibility,
        @Size(max = 3) List<@NotBlank @Size(max = 1000) String> imageUrls) {
    public MobileUpdateRamenLogCommand toCommand() {
        return new MobileUpdateRamenLogCommand(visitedAt, menuName, ramenType,
                scores == null ? null : scores.toCommand(), revisitIntention, note, tasteNoteCodes, visibility,
                imageUrls);
    }
}
