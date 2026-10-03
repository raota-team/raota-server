package com.raota.mobile.ramenlog.presentation.request;

import com.raota.mobile.ramenlog.domain.model.MobileRamenLogScores;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MobileRamenLogScoresRequest(@NotNull @Min(1) @Max(5) Integer satisfaction,
        @NotNull @Min(1) @Max(5) Integer brothDensity, @NotNull @Min(1) @Max(5) Integer noodleFirmness,
        @NotNull @Min(1) @Max(5) Integer topping) {

    public MobileRamenLogScores toScores() {
        return new MobileRamenLogScores(satisfaction, brothDensity, noodleFirmness, topping);
    }

}
