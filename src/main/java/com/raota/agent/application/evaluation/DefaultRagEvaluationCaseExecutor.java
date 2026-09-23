package com.raota.agent.application.evaluation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.raota.agent.application.ramenshop.command.AiRamenShopSearchCommand;
import com.raota.agent.application.ramenshop.query.RamenShopComparisonQuery;
import com.raota.agent.application.ramenshop.result.RamenShopComparisonDocument;
import com.raota.agent.application.ramenshop.result.AiRamenShopSearchResult;
import com.raota.agent.application.ramenshop.result.RamenShopComparisonResult;
import com.raota.agent.application.ramenshop.service.AiRamenShopSearchService;
import com.raota.agent.application.ramenshop.service.RamenShopComparisonService;
import com.raota.agent.application.recommendation.FollowUpChatService;
import com.raota.agent.application.recommendation.ReviewSummaryService;
import com.raota.agent.application.recommendation.query.FollowUpChatQuery;
import com.raota.agent.application.recommendation.query.ReviewSummaryQuery;
import com.raota.agent.presentation.recommendation.response.AiChatResponse;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse;
import com.raota.agent.application.ramenshop.result.AiRamenShopSearchResult.ShopResult;
import com.raota.agent.domain.retrieval.document.RetrievalMetadataKeys;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
public class DefaultRagEvaluationCaseExecutor implements RagEvaluationCaseExecutor {

    private final AiRamenShopSearchService searchService;

    private final ReviewSummaryService reviewSummaryService;

    private final FollowUpChatService followUpChatService;

    private final RamenShopComparisonService comparisonService;

    private final ObjectMapper objectMapper;

    public DefaultRagEvaluationCaseExecutor(AiRamenShopSearchService searchService,
            ReviewSummaryService reviewSummaryService, FollowUpChatService followUpChatService,
            RamenShopComparisonService comparisonService, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.reviewSummaryService = reviewSummaryService;
        this.followUpChatService = followUpChatService;
        this.comparisonService = comparisonService;
        this.objectMapper = objectMapper;
    }

    @Override
    public RagExecutionResult execute(RagEvaluationCase evaluationCase) {
        if (evaluationCase == null) {
            return RagExecutionResult.error(0, new IllegalArgumentException("평가 사례는 필수입니다."));
        }
        if (evaluationCase.contractOnly()) {
            return RagExecutionResult.skipped();
        }

        long startedAt = System.nanoTime();
        try {
            return switch (evaluationCase.type()) {
                case SEARCH -> executeSearch(evaluationCase, startedAt);
                case SUMMARY -> executeSummary(evaluationCase, startedAt);
                case CHAT -> executeChat(evaluationCase, startedAt);
                case COMPARE -> executeCompare(evaluationCase, startedAt);
            };
        }
        catch (Exception exception) {
            return RagExecutionResult.error(elapsedMs(startedAt), exception);
        }
    }

    private RagExecutionResult executeSearch(RagEvaluationCase evaluationCase, long startedAt) {
        JsonNode request = evaluationCase.request();
        String query = requiredText(request, "query");
        Long memberId = optionalLong(request, "memberId");
        AiRamenShopSearchResult result = searchService.search(new AiRamenShopSearchCommand(query, memberId));
        List<ShopResult> shops = result == null || result.shops() == null ? List.of() : result.shops();
        List<Long> ids = shops.stream().map(ShopResult::id).filter(java.util.Objects::nonNull).toList();
        List<RagEvaluationEvidence> evidence = shops.stream()
            .map(shop -> new RagEvaluationEvidence(shop.id() == null ? "" : shop.id().toString(), "RAMEN_SHOP",
                    "ramen-shop-search",
                    "%s %s %s".formatted(nullToEmpty(shop.name()), nullToEmpty(shop.type()),
                            nullToEmpty(shop.description())),
                    java.util.Map.of("matchScore", shop.matchScore() == null ? 0 : shop.matchScore())))
            .toList();
        return success(result, ids, evidence, isFallback(evaluationCase, result), elapsedMs(startedAt));
    }

    private RagExecutionResult executeSummary(RagEvaluationCase evaluationCase, long startedAt) {
        JsonNode request = evaluationCase.request();
        ReviewSummaryService.ReviewSummaryExecution execution = reviewSummaryService.summarizeReviewsWithEvidence(
                new ReviewSummaryQuery(requiredLong(request, "shopId"), optionalText(request, "focus")));
        ReviewSummaryResponse result = execution.response();
        List<RagEvaluationEvidence> evidence = toReviewEvidence(execution.evidence());
        return success(result, List.of(), evidence, isFallback(evaluationCase, result), elapsedMs(startedAt));
    }

    private RagExecutionResult executeChat(RagEvaluationCase evaluationCase, long startedAt) {
        JsonNode request = evaluationCase.request();
        List<FollowUpChatQuery.Message> messages = new ArrayList<>();
        JsonNode messageNodes = request == null ? null : request.get("messages");
        if (messageNodes != null && messageNodes.isArray()) {
            messageNodes.forEach(message -> messages
                .add(new FollowUpChatQuery.Message(optionalText(message, "role"), requiredText(message, "content"))));
        }
        FollowUpChatService.FollowUpChatExecution execution = followUpChatService
            .followUpChatWithEvidence(new FollowUpChatQuery(optionalText(request, "contextType"),
                    longList(request == null ? null : request.get("shopIds")), messages));
        AiChatResponse result = execution.response();
        List<RagEvaluationEvidence> evidence = toVectorEvidence(execution.evidence());
        return success(result, List.of(), evidence, isFallback(evaluationCase, result), elapsedMs(startedAt));
    }

    private RagExecutionResult executeCompare(RagEvaluationCase evaluationCase, long startedAt) {
        JsonNode request = evaluationCase.request();
        RamenShopComparisonService.RamenShopComparisonExecution execution = comparisonService
            .compareShopsWithEvidence(new RamenShopComparisonQuery(requiredLong(request, "shopAId"),
                    requiredLong(request, "shopBId"), optionalText(request, "focus")));
        RamenShopComparisonResult result = execution.response();
        List<Long> ids = result == null ? List.of()
                : java.util.stream.Stream.of(result.shopA(), result.shopB())
                    .filter(java.util.Objects::nonNull)
                    .map(RamenShopComparisonResult.ShopSummary::id)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        return success(result, ids, toComparisonEvidence(execution.evidence()), isFallback(evaluationCase, result),
                elapsedMs(startedAt));
    }

    private RagExecutionResult success(Object response, List<Long> returnedShopIds,
            List<RagEvaluationEvidence> evidence, boolean fallback, long latencyMs) {
        return new RagExecutionResult(RagEvaluationCaseStatus.COMPLETED, objectMapper.valueToTree(response),
                returnedShopIds, evidence, fallback, latencyMs, "", "");
    }

    private boolean isFallback(RagEvaluationCase evaluationCase, Object response) {
        if (response == null) {
            return true;
        }
        JsonNode json = objectMapper.valueToTree(response);
        String serialized = json.toString().toLowerCase(Locale.ROOT);
        return serialized.contains("정보 부족") || serialized.contains("데이터 부족") || serialized.contains("답변하기 어렵")
                || serialized.contains("확인 가능한 리뷰") || (evaluationCase.expectsFallback() && serialized.contains("부족"));
    }

    private String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "은(는) 필수입니다.");
        }
        return value;
    }

    private Long requiredLong(JsonNode node, String field) {
        Long value = optionalLong(node, field);
        if (value == null) {
            throw new IllegalArgumentException(field + "은(는) 필수입니다.");
        }
        return value;
    }

    private Long optionalLong(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null
                : value.isNumber() ? value.longValue() : Long.valueOf(value.asText());
    }

    private List<Long> longList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<Long> values = new ArrayList<>();
        node.forEach(item -> values.add(item.longValue()));
        return values;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private List<RagEvaluationEvidence> toReviewEvidence(List<Document> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
            .map(document -> new RagEvaluationEvidence(metadataText(document, RetrievalMetadataKeys.SOURCE_ID),
                    "EXTERNAL_REVIEW", metadataText(document, RetrievalMetadataKeys.SOURCE),
                    document == null ? "" : document.getText(),
                    safeMetadata(document == null ? null : document.getMetadata())))
            .toList();
    }

    private List<RagEvaluationEvidence> toVectorEvidence(List<Document> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
            .map(document -> new RagEvaluationEvidence(metadataText(document, RetrievalMetadataKeys.SOURCE_ID),
                    metadataText(document, RetrievalMetadataKeys.DOCUMENT_TYPE),
                    metadataText(document, RetrievalMetadataKeys.SOURCE), document == null ? "" : document.getText(),
                    safeMetadata(document == null ? null : document.getMetadata())))
            .toList();
    }

    private List<RagEvaluationEvidence> toComparisonEvidence(List<RamenShopComparisonDocument> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
            .map(document -> new RagEvaluationEvidence(
                    metadataText(document == null ? null : document.metadata(), RetrievalMetadataKeys.SHOP_ID),
                    metadataText(document == null ? null : document.metadata(), RetrievalMetadataKeys.DOCUMENT_TYPE),
                    metadataText(document == null ? null : document.metadata(), RetrievalMetadataKeys.SOURCE),
                    document == null ? "" : document.text(),
                    safeMetadata(document == null ? null : document.metadata())))
            .toList();
    }

    private String metadataText(Document document, String key) {
        return metadataText(document == null ? null : document.getMetadata(), key);
    }

    private String metadataText(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? "" : value.toString();
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                safe.put(key, value);
            }
        });
        return safe;
    }

}
