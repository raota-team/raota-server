package com.raota.mobile.ramenlog.domain.model;

/** 신고 처리 상태다. 접수는 OPEN이고 처리·기각은 관리자 기능에서 바꾼다. */
public enum ReportStatus {

    OPEN, RESOLVED, DISMISSED

}
