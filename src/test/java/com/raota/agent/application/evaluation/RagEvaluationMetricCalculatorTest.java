package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

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

    @Test
    void calculatesGenerationCoverageAndFallbackAccuracy() throws Exception {
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                "summary-1",
                RagEvaluationCaseType.SUMMARY,
                RagEvaluationSplit.DEV,
                JsonMapper.builder().build().createObjectNode().put("shopId", 1),
                "AUTHENTICATED",
                List.of(),
                false,
                List.of("시오라멘", "웨이팅"),
                List.of(),
                List.of("확정할 수 없는 가격"),
                false,
                false,
                1,
                6
        );

        var response = JsonMapper.builder().build().readTree("""
                {
                  "shopInfo": {"id": 1, "name": "라멘집", "type": "시오", "location": "서울", "imageUrl": "", "isBookmarked": false},
                  "reviewCount": 2,
                  "summary": {
                    "pros": {"title": "장점", "body": "시오라멘은 깔끔합니다."},
                    "cons": {"title": "단점", "body": "웨이팅이 있습니다."},
                    "recommendedMenu": {"title": "추천 메뉴", "body": "시오라멘"}
                  },
                  "sampleReviews": []
                }
                """);

        Map<String, Double> metrics = RagEvaluationMetricCalculator.calculateGeneration(
                evaluationCase,
                response,
                false
        );

        assertThat(metrics).containsEntry("schemaValid", 1.0)
                .containsEntry("requiredFactCoverage", 1.0)
                .containsEntry("forbiddenClaimRate", 0.0)
                .containsEntry("fallbackAccuracy", 1.0);
    }

    @Test
    void rejectsGenerationResponseWithWrongShape() throws Exception {
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                "chat-1", RagEvaluationCaseType.CHAT, RagEvaluationSplit.DEV,
                JsonMapper.builder().build().createObjectNode(), "AUTHENTICATED",
                List.of(), false, List.of(), List.of(), List.of(), false, false, 1, 6
        );

        Map<String, Double> metrics = RagEvaluationMetricCalculator.calculateGeneration(
                evaluationCase,
                JsonMapper.builder().build().readTree("{\"body\":\"답변\"}"),
                false
        );

        assertThat(metrics).containsEntry("schemaValid", 0.0);
    }
}
