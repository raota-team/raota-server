package com.raota.mobile.account.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 탈퇴 사유와 회원의 명시적인 확인을 요구한다. */
public record MobileWithdrawalRequest(@NotBlank @Size(max = 50) String reasonCode,
        @NotNull @Pattern(regexp = "WITHDRAW", message = "WITHDRAW를 입력해 주세요.") String confirmation) {
}
