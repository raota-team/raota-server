package com.raota.domain.recommendation.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComparisonScoresTest {

    @Test
    void from_should_use_default_scores_when_raw_scores_are_null() {
        ComparisonScores scores = ComparisonScores.from(null);

        assertThat(scores.asMap())
                .containsExactly(
                        Map.entry("soup", 50),
                        Map.entry("noodle", 50),
                        Map.entry("topping", 50),
                        Map.entry("mood", 50),
                        Map.entry("access", 50),
                        Map.entry("revisit", 50)
                );
        assertThat(scores.totalIndex()).isEqualTo(50.0);
    }

    @Test
    void from_should_fill_missing_scores_with_default_score() {
        ComparisonScores scores = ComparisonScores.from(Map.of(
                "soup", 85,
                "access", 70
        ));

        assertThat(scores.asMap())
                .containsExactly(
                        Map.entry("soup", 85),
                        Map.entry("noodle", 50),
                        Map.entry("topping", 50),
                        Map.entry("mood", 50),
                        Map.entry("access", 70),
                        Map.entry("revisit", 50)
                );
    }

    @Test
    void from_should_clamp_scores_to_zero_to_one_hundred() {
        ComparisonScores scores = ComparisonScores.from(Map.of(
                "soup", -10,
                "noodle", 110,
                "topping", 80,
                "mood", 75,
                "access", 0,
                "revisit", 100
        ));

        assertThat(scores.asMap())
                .containsEntry("soup", 0)
                .containsEntry("noodle", 100)
                .containsEntry("topping", 80)
                .containsEntry("mood", 75)
                .containsEntry("access", 0)
                .containsEntry("revisit", 100);
    }

    @Test
    void totalIndex_should_round_average_to_one_decimal_place() {
        Map<String, Integer> rawScores = new LinkedHashMap<>();
        rawScores.put("soup", 100);
        rawScores.put("noodle", 82);
        rawScores.put("topping", 50);
        rawScores.put("mood", 0);
        rawScores.put("access", 50);
        rawScores.put("revisit", 50);

        ComparisonScores scores = ComparisonScores.from(rawScores);

        assertThat(scores.totalIndex()).isEqualTo(55.3);
    }
}
