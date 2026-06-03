package com.raota.application.recommendation;

import com.raota.application.recommendation.dto.AiReviewSummaryResult;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.retrieval.document.RetrievalDocumentType;
import com.raota.domain.retrieval.document.RetrievalMetadataKeys;
import com.raota.presentation.api.recommendation.request.ReviewSummaryRequest;
import com.raota.presentation.api.recommendation.response.ReviewSummaryResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ReviewSummaryService {
    private static final int SAMPLE_REVIEW_LIMIT = 3;
    private static final int SAMPLE_REVIEW_MAX_LENGTH = 160;

    private final RecommendationShopReader recommendationShopReader;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final Resource reviewSummaryTemplate;

    public ReviewSummaryService(
            RecommendationShopReader recommendationShopReader,
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/system-persona.st") Resource systemPersona,
            @Value("classpath:/prompts/review-summary.st") Resource reviewSummaryTemplate
    ) {
        this.recommendationShopReader = recommendationShopReader;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.defaultSystem(systemPersona).build();
        this.reviewSummaryTemplate = reviewSummaryTemplate;
    }

    public ReviewSummaryResponse summarizeReviews(ReviewSummaryRequest request) {
        validateReviewSummaryRequest(request);

        RamenShop ramenShop = recommendationShopReader.getRamenShop(request.shopId());
        String focus = recommendationShopReader.normalizeText(request.focus());

        List<Document> reviewDocuments = collectReviewDocuments(ramenShop, focus);

        if (hasInsufficientReviewDocuments(reviewDocuments)) {
            return buildFallbackResponse(ramenShop);
        }

        AiReviewSummaryResult aiResult = generateReviewSummaryResult(focus, ramenShop, reviewDocuments);

        return buildReviewSummaryResponse(ramenShop, reviewDocuments, aiResult);
    }

    private void validateReviewSummaryRequest(ReviewSummaryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("리뷰 요약 요청은 필수입니다.");
        }

        if (request.shopId() == null) {
            throw new IllegalArgumentException("리뷰를 요약할 매장 ID는 필수입니다.");
        }
    }

    private ReviewSummaryResponse buildFallbackResponse(RamenShop shop) {
        return new ReviewSummaryResponse(
                buildShopInfo(shop),
                0,
                buildFallbackSummary(),
                List.of()
        );
    }

    private ReviewSummaryResponse buildReviewSummaryResponse(
            RamenShop shop,
            List<Document> reviewDocuments,
            AiReviewSummaryResult aiResult
    ) {
        return new ReviewSummaryResponse(
                buildShopInfo(shop),
                reviewDocuments.size(),
                toSummary(aiResult),
                toSampleReviews(reviewDocuments)
        );
    }

    private ReviewSummaryResponse.AiShopBasicInfo buildShopInfo(RamenShop shop) {
        return new ReviewSummaryResponse.AiShopBasicInfo(
                shop.getId(),
                shop.getName(),
                recommendationShopReader.primaryTag(shop),
                recommendationShopReader.addressText(shop),
                shop.getImageUrl(),
                false
        );
    }

    private ReviewSummaryResponse.AiSummary buildFallbackSummary() {
        return new ReviewSummaryResponse.AiSummary(
                new ReviewSummaryResponse.SummaryDetail(
                        "리뷰 데이터 부족",
                        "요약할 수 있는 리뷰 데이터가 충분하지 않습니다."
                ),
                new ReviewSummaryResponse.SummaryDetail(
                        "리뷰 데이터 부족",
                        "단점을 판단할 수 있는 리뷰 데이터가 충분하지 않습니다."
                ),
                new ReviewSummaryResponse.SummaryDetail(
                        "추천 메뉴 정보 부족",
                        "추천 메뉴를 판단할 수 있는 리뷰 데이터가 충분하지 않습니다."
                )
        );
    }

    private ReviewSummaryResponse.AiSummary toSummary(AiReviewSummaryResult aiResult) {
        if (aiResult == null || aiResult.summary() == null) {
            return buildFallbackSummary();
        }

        return new ReviewSummaryResponse.AiSummary(
                toSummaryDetail(
                        aiResult.summary().pros(),
                        "장점 정보 부족",
                        "장점을 판단할 수 있는 리뷰 데이터가 충분하지 않습니다."
                ),
                toSummaryDetail(
                        aiResult.summary().cons(),
                        "단점 정보 부족",
                        "단점을 판단할 수 있는 리뷰 데이터가 충분하지 않습니다."
                ),
                toSummaryDetail(
                        aiResult.summary().recommendedMenu(),
                        "추천 메뉴 정보 부족",
                        "추천 메뉴를 판단할 수 있는 리뷰 데이터가 충분하지 않습니다."
                )
        );
    }

    private ReviewSummaryResponse.SummaryDetail toSummaryDetail(
            AiReviewSummaryResult.AiSummaryDetail detail,
            String fallbackTitle,
            String fallbackBody
    ) {
        if (detail == null || !recommendationShopReader.hasText(detail.title())
                || !recommendationShopReader.hasText(detail.body())) {
            return new ReviewSummaryResponse.SummaryDetail(fallbackTitle, fallbackBody);
        }

        return new ReviewSummaryResponse.SummaryDetail(
                detail.title().trim(),
                detail.body().trim()
        );
    }

    private List<ReviewSummaryResponse.SampleReview> toSampleReviews(List<Document> reviewDocuments) {
        return reviewDocuments.stream()
                .limit(SAMPLE_REVIEW_LIMIT)
                .map(this::toSampleReview)
                .toList();
    }

    private ReviewSummaryResponse.SampleReview toSampleReview(Document document) {
        Object sourceId = document.getMetadata().get(RetrievalMetadataKeys.SOURCE_ID);

        return new ReviewSummaryResponse.SampleReview(
                sourceId == null ? "익명 리뷰" : "리뷰 " + sourceId,
                null,
                truncate(document.getText(), SAMPLE_REVIEW_MAX_LENGTH)
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() <= maxLength) {
            return trimmedValue;
        }

        return trimmedValue.substring(0, maxLength).trim() + "...";
    }

    private List<Document> collectReviewDocuments(RamenShop shop, String focus) {
        String query = buildReviewSummaryQuery(shop, focus);

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.eq(RetrievalMetadataKeys.SHOP_ID, String.valueOf(shop.getId())),
                builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.REVIEW_CHUNK.name())
        ).build();

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(12)
                        .similarityThreshold(0.45)
                        .filterExpression(filter)
                        .build()
        );
    }

    private String buildReviewSummaryQuery(RamenShop shop, String focus) {
        if (recommendationShopReader.hasText(focus)) {
            return "%s %s 리뷰 장점 단점 추천 메뉴".formatted(shop.getName(), focus);
        }

        return "%s 리뷰 장점 단점 추천 메뉴 맛 국물 면 토핑 웨이팅 분위기 재방문".formatted(shop.getName());
    }

    private boolean hasInsufficientReviewDocuments(List<Document> reviewDocuments) {
        return reviewDocuments == null || reviewDocuments.isEmpty();
    }

    private AiReviewSummaryResult generateReviewSummaryResult(
            String focus,
            RamenShop shop,
            List<Document> reviewDocuments
    ) {
        return chatClient.prompt()
                .user(user -> user.text(reviewSummaryTemplate)
                        .param("focus", recommendationShopReader.hasText(focus) ? focus : "전반적인 리뷰 요약")
                        .param("shopInfo", recommendationShopReader.buildShopInfoContext(shop))
                        .param("reviewContext", buildReviewContext(reviewDocuments)))
                .call()
                .entity(AiReviewSummaryResult.class);
    }

    private String buildReviewContext(List<Document> reviewDocuments) {
        return reviewDocuments.stream()
                .map(this::formatReviewDocument)
                .collect(Collectors.joining("\n"));
    }

    private String formatReviewDocument(Document document) {
        Object sourceId = document.getMetadata().get(RetrievalMetadataKeys.SOURCE_ID);
        Object createdAt = document.getMetadata().get(RetrievalMetadataKeys.CREATED_AT);

        return """
            - 리뷰ID: %s
              작성일: %s
              내용: %s
            """.formatted(
                sourceId == null ? "UNKNOWN" : sourceId,
                createdAt == null ? "UNKNOWN" : createdAt,
                document.getText()
        );
    }

}
