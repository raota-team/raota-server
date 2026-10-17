package com.raota.agent.application.mobile.command;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 앱 AI 큐레이터가 고른 취향 조건과 자유 입력, 위치 조건이다. 빈 값은 조건이 없다는 뜻이다. */
public record MobileRecommendationCommand(String soup, String mood, String priority, String text, BigDecimal latitude,
        BigDecimal longitude, String region) {

    /** 취향 조건과 자유 입력을 순서대로 이은 검색 문장이다. */
    public String query() {
        return String.join(" ", preferences(true));
    }

    /** 화면에 그대로 보여줄 취향 조건이다. 자유 입력은 넣지 않는다. */
    public List<String> preferenceLabels() {
        return preferences(false);
    }

    private List<String> preferences(boolean withText) {
        List<String> values = new ArrayList<>();
        for (String value : List.of(blankToEmpty(soup), blankToEmpty(mood), blankToEmpty(priority))) {
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        if (withText && !blankToEmpty(text).isEmpty()) {
            values.add(text.trim());
        }
        return values;
    }

    private static String blankToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

}
