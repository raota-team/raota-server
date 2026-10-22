package com.raota.agent.application.mobile.command;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 앱 AI 큐레이터가 고른 국물·상황·우선순위와 자유 입력, 위치 조건이다. 빈 값은 조건이 없다는 뜻이다. */
public record MobileRecommendationCommand(String soup, String mood, String priority, String text, BigDecimal latitude,
        BigDecimal longitude, String region) {

    /** 국물·상황·우선순위·자유 입력 중 하나라도 있는지다. */
    public boolean hasCondition() {
        return soupType() != null || !similarityQuery().isEmpty();
    }

    /** 매장 라멘 종류로 거르는 국물이다. 없으면 {@code null}이다. */
    public String soupType() {
        String value = blankToEmpty(soup);
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }

    /** 매장 문서와 비교할 문장이다. 국물은 종류로 거르므로 넣지 않고 상황·우선순위·자유 입력을 잇는다. */
    public String similarityQuery() {
        List<String> values = new ArrayList<>();
        for (String value : List.of(blankToEmpty(mood), blankToEmpty(priority), blankToEmpty(text))) {
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return String.join(" ", values);
    }

    /** 화면에 그대로 보여줄 취향 조건이다. 자유 입력은 넣지 않는다. */
    public List<String> preferenceLabels() {
        List<String> values = new ArrayList<>();
        for (String value : List.of(blankToEmpty(soup), blankToEmpty(mood), blankToEmpty(priority))) {
            if (!value.isEmpty()) {
                values.add(value);
            }
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
