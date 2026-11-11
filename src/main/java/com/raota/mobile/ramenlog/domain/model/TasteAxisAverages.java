package com.raota.mobile.ramenlog.domain.model;

/**
 * 점수 넷과 재방문 의사가 모두 있는 기록의 다섯 축 평균(반올림 전)이다. 축 이름은 기록 응답의 {@code scores}와 같고, 재방문은
 * {@link RevisitIntention#score()}로 센다. 그런 기록이 없으면 다섯 축 모두 null이다.
 */
public record TasteAxisAverages(
        int scoredCount,
        Double satisfaction,
        Double brothDensity,
        Double noodleFirmness,
        Double topping,
        Double revisit) {}
