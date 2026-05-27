package com.raota.domain.recommendation.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ComparisonScores {
    private static final int DEFAULT_SCORE = 50;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private final Map<String, Integer> scores;

    private ComparisonScores(Map<String, Integer> scores) {
        this.scores = Collections.unmodifiableMap(new LinkedHashMap<>(scores));
    }

    public static ComparisonScores from(Map<String, Integer> rawScores) {
        Map<String, Integer> normalizedScores = new LinkedHashMap<>();

        for (ComparisonScoreType type : ComparisonScoreType.values()) {
            Integer rawScore = rawScores == null ? null : rawScores.get(type.key());
            normalizedScores.put(type.key(), normalize(rawScore));
        }

        return new ComparisonScores(normalizedScores);
    }

    public Map<String, Integer> asMap() {
        return scores;
    }

    public double totalIndex() {
        double average = scores.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(DEFAULT_SCORE);

        return BigDecimal.valueOf(average)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static Integer normalize(Integer score) {
        if (score == null) {
            return DEFAULT_SCORE;
        }

        if (score < MIN_SCORE) {
            return MIN_SCORE;
        }

        if (score > MAX_SCORE) {
            return MAX_SCORE;
        }

        return score;
    }
}
