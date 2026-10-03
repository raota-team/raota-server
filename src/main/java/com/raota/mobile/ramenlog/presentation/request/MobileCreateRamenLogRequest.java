package com.raota.mobile.ramenlog.presentation.request;

import com.raota.mobile.ramenlog.application.command.MobileCreateRamenLogCommand;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record MobileCreateRamenLogRequest(@NotBlank @Pattern(regexp = "[0-9]+") String shopId,
        @NotNull LocalDate visitedAt, @NotBlank @Size(max = 150) String menuName, @NotBlank String ramenType,
        @NotNull @Valid MobileRamenLogScoresRequest scores, @NotNull RevisitIntention revisitIntention,
        @NotNull @Size(max = 500) String note, @NotNull List<@NotBlank String> tasteNoteCodes,
        @NotNull LogVisibility visibility, @NotNull @Size(max = 3) List<@NotBlank @Size(max = 1000) String> imageUrls) {

    public MobileCreateRamenLogCommand toCommand() {
        return new MobileCreateRamenLogCommand(shopId, visitedAt, menuName, ramenType, scores.toScores(),
                revisitIntention, note, tasteNoteCodes, visibility, imageUrls);
    }

}
