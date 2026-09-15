package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.raota.agent.application.ramenshop.result.AiRamenShopSearchResult;
import com.raota.agent.application.ramenshop.command.AiRamenShopSearchCommand;
import com.raota.agent.application.ramenshop.service.AiRamenShopSearchService;
import com.raota.agent.application.ramenshop.service.RamenShopComparisonService;
import com.raota.agent.application.recommendation.FollowUpChatService;
import com.raota.agent.application.recommendation.ReviewSummaryService;
import com.raota.agent.application.recommendation.query.ReviewSummaryQuery;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class DefaultRagEvaluationCaseExecutorTest {

    private final AiRamenShopSearchService searchService = mock(AiRamenShopSearchService.class);
    private final ReviewSummaryService reviewSummaryService = mock(ReviewSummaryService.class);
    private final FollowUpChatService followUpChatService = mock(FollowUpChatService.class);
    private final RamenShopComparisonService comparisonService = mock(RamenShopComparisonService.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final DefaultRagEvaluationCaseExecutor executor = new DefaultRagEvaluationCaseExecutor(
            searchService,
            reviewSummaryService,
            followUpChatService,
            comparisonService,
            objectMapper
    );

    @Test
    void executesSearchAndExposesCandidateIdsAndEvidence() {
        when(searchService.search(new AiRamenShopSearchCommand("시오 국물", null))).thenReturn(new AiRamenShopSearchResult(List.of(
                new AiRamenShopSearchResult.ShopResult(7L, "라멘집", "시오", "서울", "담백한 국물", "", 92, false)
        )));

        RagExecutionResult result = executor.execute(new RagEvaluationCase(
                "search-1", RagEvaluationCaseType.SEARCH, RagEvaluationSplit.DEV,
                objectMapper.createObjectNode().put("query", "시오 국물"), "ANONYMOUS",
                List.of(new RagExpectedShop(7L, 3)), false, List.of(), List.of(), false, false, 1, 6
        ));

        assertThat(result.status()).isEqualTo(RagEvaluationCaseStatus.COMPLETED);
        assertThat(result.returnedShopIds()).containsExactly(7L);
        assertThat(result.evidence()).singleElement().satisfies(evidence ->
                assertThat(evidence.text()).contains("담백한 국물"));
    }

    @Test
    void skipsContractOnlyCaseWithoutCallingProductServices() {
        RagExecutionResult result = executor.execute(new RagEvaluationCase(
                "search-contract", RagEvaluationCaseType.SEARCH, RagEvaluationSplit.DEV,
                objectMapper.createObjectNode().put("query", "거리 가까운 곳"), "AUTHENTICATED",
                List.of(), false, List.of(), List.of(), false, true, 1, 6
        ));

        assertThat(result.status()).isEqualTo(RagEvaluationCaseStatus.SKIPPED);
        verifyNoInteractions(searchService, reviewSummaryService, followUpChatService, comparisonService);
    }

    @Test
    void recognizesReviewSummaryFallbackResponse() {
        when(reviewSummaryService.summarizeReviews(org.mockito.ArgumentMatchers.any(ReviewSummaryQuery.class))).thenReturn(
                new ReviewSummaryResponse(
                        new ReviewSummaryResponse.AiShopBasicInfo(7L, "라멘집", "시오", "서울", "", false),
                        0,
                        new ReviewSummaryResponse.AiSummary(
                                new ReviewSummaryResponse.SummaryDetail("리뷰 데이터 부족", "리뷰가 없습니다."),
                                new ReviewSummaryResponse.SummaryDetail("리뷰 데이터 부족", "리뷰가 없습니다."),
                                new ReviewSummaryResponse.SummaryDetail("추천 메뉴 정보 부족", "리뷰가 없습니다.")
                        ),
                        List.of()
                )
        );

        RagExecutionResult result = executor.execute(new RagEvaluationCase(
                "summary-1", RagEvaluationCaseType.SUMMARY, RagEvaluationSplit.DEV,
                objectMapper.createObjectNode().put("shopId", 7), "AUTHENTICATED",
                List.of(), false, List.of("리뷰"), List.of(), true, false, 1, 6
        ));

        assertThat(result.fallback()).isTrue();
        assertThat(result.response().toString()).contains("리뷰 데이터 부족");
    }
}
