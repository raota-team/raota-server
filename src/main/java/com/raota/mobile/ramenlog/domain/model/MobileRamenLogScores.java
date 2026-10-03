package com.raota.mobile.ramenlog.domain.model;

/** 기록의 네 가지 맛 점수(1~5)다. */
public record MobileRamenLogScores(int satisfaction, int brothDensity, int noodleFirmness, int topping) {
}
