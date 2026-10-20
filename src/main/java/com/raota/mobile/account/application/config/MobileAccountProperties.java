package com.raota.mobile.account.application.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 탈퇴 요청 후 계정 정보를 보관할 유예 기간과, 회원이 동의해야 하는 이용약관·개인정보처리방침 버전(시행일 YYYY-MM-DD)이다. 문서를 개정하면 버전을
 * 올려 기존 회원에게 다시 동의를 받는다.
 */
@ConfigurationProperties("app.mobile.account")
@Validated
public record MobileAccountProperties(@NotNull @DefaultValue("30d") Duration withdrawalGracePeriod,
        @NotNull @Pattern(
                regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}") @DefaultValue("2026-06-11") String requiredTermsVersion,
        @NotNull @Pattern(
                regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}") @DefaultValue("2026-10-27") String requiredPrivacyVersion) {

    @AssertTrue(message = "탈퇴 유예 기간은 양수여야 합니다.")
    public boolean isWithdrawalGracePeriodPositive() {
        return withdrawalGracePeriod != null && withdrawalGracePeriod.compareTo(Duration.ZERO) > 0;
    }

}
