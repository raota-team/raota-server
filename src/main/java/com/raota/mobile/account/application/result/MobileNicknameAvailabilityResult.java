package com.raota.mobile.account.application.result;

/** 입력을 정리한 표시용 닉네임과 현재 사용 가능 여부다. */
public record MobileNicknameAvailabilityResult(String nickname, boolean available) {
}
