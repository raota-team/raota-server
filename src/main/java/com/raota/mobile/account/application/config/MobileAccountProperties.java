package com.raota.mobile.account.application.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** 탈퇴 요청 후 계정 정보를 보관할 유예 기간이다. */
@ConfigurationProperties("app.mobile.account")
@Validated
public record MobileAccountProperties(@NotNull @DefaultValue("30d") Duration withdrawalGracePeriod) {

    @AssertTrue(message = "탈퇴 유예 기간은 양수여야 합니다.")
    public boolean isWithdrawalGracePeriodPositive() {
        return withdrawalGracePeriod != null && withdrawalGracePeriod.compareTo(Duration.ZERO) > 0;
    }

}
