package com.raota.mobile.ramenlog.application.result;

/** 앱의 `BlockedMember`와 같은 형태의 차단한 회원이다. */
public record MobileBlockedMemberResult(String id, String nickname, String avatarUrl) {
}
