package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RagEvaluationMetricCalculatorTest {

    @Test
    void calculatesMobileTopOneAndDiagnosticTopSixMetrics() {
        Map<String, Double> metrics = RagEvaluationMetricCalculator.calculateSearch(
                List.of(3L, 2L, 99L),
                List.of(new RagExpectedShop(2L, 3), new RagExpectedShop(3L, 1)),
                false,
                1,
                6
        );

        assertThat(metrics.get("hitrateAt1")).isEqualTo(1.0);
        assertThat(metrics.get("recallAt1")).isEqualTo(0.5);
        assertThat(metrics.get("hitrateAt6")).isEqualTo(1.0);
        assertThat(metrics.get("recallAt6")).isEqualTo(1.0);
        assertThat(metrics.get("precisionAt6")).isEqualTo(2.0 / 6.0);
        assertThat(metrics.get("mrrAt6")).isEqualTo(1.0);
        assertThat(metrics.get("ndcgAt1")).isEqualTo(1.0 / 7.0);
    }

    @Test
    void calculatesEmptyResultAccuracyWithoutGoldItems() {
        assertThat(RagEvaluationMetricCalculator.calculateSearch(
                List.of(), List.of(), true, 1, 6
        )).containsEntry("emptyResultAccuracy", 1.0);

        assertThat(RagEvaluationMetricCalculator.calculateSearch(
                List.of(1L), List.of(), true, 1, 6
        )).containsEntry("emptyResultAccuracy", 0.0);
    }

    @Test
    void averagesEachMetricAcrossCases() {
        Map<String, Double> average = RagEvaluationMetricCalculator.average(List.of(
                Map.of("hitrateAt1", 1.0, "recallAt6", 0.0),
                Map.of("hitrateAt1", 0.0, "recallAt6", 1.0)
        ));

        assertThat(average).containsEntry("hitrateAt1", 0.5)
                .containsEntry("recallAt6", 0.5);
    }
}
