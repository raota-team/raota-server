package com.raota.mobile.account.domain.event;

/** 회원 익명화와 같은 트랜잭션에서 관련 모듈의 개인 데이터를 정리하라는 사건이다. */
public record MobileMemberPurgedEvent(Long userId) {
}
