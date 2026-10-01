package com.raota.mobile.account.application.result;

import com.raota.mobile.account.domain.model.MobileUserStatus;
import java.time.Instant;

/** 탈퇴 요청이 유예 중인 계정의 처리 기한이다. */
public record MobileWithdrawalResult(MobileUserStatus status, Instant purgeScheduledAt) {
}
