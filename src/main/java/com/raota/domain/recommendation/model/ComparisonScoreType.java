package com.raota.domain.recommendation.model;

import java.util.Arrays;
import java.util.List;

public enum ComparisonScoreType {
    SOUP("soup"),
    NOODLE("noodle"),
    TOPPING("topping"),
    MOOD("mood"),
    ACCESS("access"),
    REVISIT("revisit");

    private final String key;

    ComparisonScoreType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static List<String> keys() {
        return Arrays.stream(values())
                .map(ComparisonScoreType::key)
                .toList();
    }
}
