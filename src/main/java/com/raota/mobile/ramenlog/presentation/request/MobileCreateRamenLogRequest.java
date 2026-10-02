package com.raota.mobile.ramenlog.presentation.request;

import com.raota.mobile.ramenlog.application.command.MobileCreateRamenLogCommand;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record MobileCreateRamenLogRequest(@NotBlank @Pattern(regexp = "[0-9]+") String shopId,
        @NotNull LocalDate visitedAt, @NotBlank @Size(max = 150) String menuName, @NotBlank String ramenType,
        @NotNull @Valid Scores scores, @NotNull RevisitIntention revisitIntention,
        @NotNull @Size(max = 500) String note, @NotNull List<@NotBlank String> tasteNoteCodes,
        @NotNull LogVisibility visibility, @NotNull @Size(max = 3) List<@NotBlank @Size(max = 1000) String> imageUrls) {

    public MobileCreateRamenLogCommand toCommand() {
        return new MobileCreateRamenLogCommand(shopId, visitedAt, menuName, ramenType, scores.toCommand(),
                revisitIntention, note, tasteNoteCodes, visibility, imageUrls);
    }

    public record Scores(@NotNull @Min(1) @Max(5) Integer satisfaction, @NotNull @Min(1) @Max(5) Integer brothDensity,
            @NotNull @Min(1) @Max(5) Integer noodleFirmness, @NotNull @Min(1) @Max(5) Integer topping) {
        public MobileCreateRamenLogCommand.Scores toCommand() {
            return new MobileCreateRamenLogCommand.Scores(satisfaction, brothDensity, noodleFirmness, topping);
        }
    }

}
