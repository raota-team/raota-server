package com.raota.agent.presentation.mobile.request;

import com.raota.agent.application.mobile.command.MobileRecommendationCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 기기 GPS 좌표는 소수점 자릿수가 길어 자릿수는 제한하지 않고, 매장 좌표의 저장 정밀도(소수점 8자리)로 반올림한다. */
public record MobileRecommendationRequest(@Size(max = 50) String soup, @Size(max = 50) String mood,
        @Size(max = 50) String priority, @Size(max = 200) String text,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude, @Size(max = 30) String region) {

    public MobileRecommendationCommand toCommand() {
        return new MobileRecommendationCommand(soup, mood, priority, text, rounded(latitude), rounded(longitude),
                region);
    }

    private static BigDecimal rounded(BigDecimal coordinate) {
        if (coordinate == null) {
            return null;
        }
        return coordinate.setScale(8, RoundingMode.HALF_UP);
    }

}
