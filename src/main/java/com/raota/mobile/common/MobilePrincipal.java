package com.raota.mobile.common;

/** 인증된 모바일 회원을 다른 v2 모듈에 노출하는 최소 식별자다. */
public interface MobilePrincipal {

    Long userId();

}
