package com.raota.mobile.ramenlog.application.command;

/** 관리자의 신고 처리 방식이다. */
public enum MobileReportResolution {

    /** 대상 기록·댓글을 지우고 같은 대상의 열린 신고를 처리 완료로 닫는다. */
    DELETE_CONTENT,

    /** 대상은 두고 같은 대상의 열린 신고를 기각으로 닫는다. */
    DISMISS

}
