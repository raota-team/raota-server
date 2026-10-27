package com.raota.global.security;

/** 인증 주체에서 회원 ID를 꺼내는 공통 interface다. 기능 모듈의 인증 주체 타입을 global이 몰라도 되게 한다. */
public interface IdentifiedPrincipal {

    Long memberId();
}
