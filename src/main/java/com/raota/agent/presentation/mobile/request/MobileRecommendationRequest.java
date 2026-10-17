package com.raota.agent.presentation.mobile.request;

import com.raota.agent.application.mobile.command.MobileRecommendationCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MobileRecommendationRequest(@Size(max = 50) String soup, @Size(max = 50) String mood,
        @Size(max = 50) String priority, @Size(max = 200) String text,
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 10) BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 10) BigDecimal longitude,
        @Size(max = 30) String region) {

    public MobileRecommendationCommand toCommand() {
        return new MobileRecommendationCommand(soup, mood, priority, text, latitude, longitude, region);
    }

}
