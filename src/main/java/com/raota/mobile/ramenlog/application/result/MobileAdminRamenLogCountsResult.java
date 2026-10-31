package com.raota.mobile.ramenlog.application.result;

/** 관리자에게 보여 주는 한 회원의 삭제되지 않은 기록 수다. */
public record MobileAdminRamenLogCountsResult(String userId, long publicCount, long privateCount) {}
