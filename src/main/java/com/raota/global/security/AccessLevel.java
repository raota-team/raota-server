package com.raota.global.security;

public enum AccessLevel {

    PUBLIC, AUTHENTICATED, ACTIVE_MEMBER, ADMIN;

    /** 온보딩을 완료한 모바일 회원이 보유하는 권한 이름이다. */
    public static final String ACTIVE_MEMBER_AUTHORITY = "ACTIVE_MEMBER";

}
