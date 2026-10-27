package com.raota.mobile.ramenlog.application.result;

/** 공감 변경 뒤 요청한 회원의 공감 여부와 기록의 공감 수다. */
public record MobileRamenLogLikeResult(boolean liked, int likeCount) {}
