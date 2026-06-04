package com.raota.application.recommendation;

import com.raota.application.recommendation.dto.AiShopComparisonResult;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.recommendation.model.ComparisonScores;
import com.raota.domain.retrieval.document.RetrievalDocumentType;
import com.raota.domain.retrieval.document.RetrievalMetadataKeys;
import com.raota.presentation.api.recommendation.request.ShopComparisonRequest;
import com.raota.presentation.api.recommendation.response.ShopComparisonResponse;
import java.util.List;
import java.util.Objects;
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
public class ShopComparisonService {

    private final RecommendationShopReader recommendationShopReader;
    private final ChatClient chatClient;
    private final Resource compareShopsTemplate;
    private final VectorStore vectorStore;

    public ShopComparisonService(
            RecommendationShopReader recommendationShopReader,
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/system-persona.st") Resource systemPersona,
            @Value("classpath:/prompts/compare-shops.st") Resource compareShopsTemplate
    ) {
        this.recommendationShopReader = recommendationShopReader;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.defaultSystem(systemPersona).build();
        this.compareShopsTemplate = compareShopsTemplate;
    }

    public ShopComparisonResponse compareShops(ShopComparisonRequest request) {
        validateComparisonRequest(request);

        RamenShop shopA = recommendationShopReader.getRamenShop(request.shopAId());
        RamenShop shopB = recommendationShopReader.getRamenShop(request.shopBId());

        String focus = recommendationShopReader.normalizeText(request.focus());

        List<Document> shopADocuments = collectComparisonDocuments(shopA, focus);
        List<Document> shopBDocuments = collectComparisonDocuments(shopB, focus);

        if (hasInsufficientDocuments(shopADocuments, shopBDocuments)) {
            return buildComparisonResponse(shopA, shopB, null);
        }

        String contextA = buildComparisonContext(shopA, shopADocuments);
        String contextB = buildComparisonContext(shopB, shopBDocuments);

        AiShopComparisonResult aiResult = generateComparisonResult(focus, contextA, contextB);

        return buildComparisonResponse(shopA, shopB, aiResult);
    }

    private void validateComparisonRequest(ShopComparisonRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("비교 요청은 필수입니다.");
        }

        if (request.shopAId() == null) {
            throw new IllegalArgumentException("비교할 첫 번째 매장 ID는 필수입니다.");
        }

        if (request.shopBId() == null) {
            throw new IllegalArgumentException("비교할 두 번째 매장 ID는 필수입니다.");
        }

        if (request.shopAId().equals(request.shopBId())) {
            throw new IllegalArgumentException("서로 다른 두 매장을 선택해야 합니다.");
        }
    }

    private ShopComparisonResponse buildComparisonResponse(
            RamenShop shopA,
            RamenShop shopB,
            AiShopComparisonResult aiResult
    ) {
        ComparisonScores shopAScores = ComparisonScores.from(
                aiResult == null || aiResult.shopA() == null ? null : aiResult.shopA().scores()
        );
        ComparisonScores shopBScores = ComparisonScores.from(
                aiResult == null || aiResult.shopB() == null ? null : aiResult.shopB().scores()
        );

        return new ShopComparisonResponse(
                new ShopComparisonResponse.ShopComparisonDetail(
                        shopA.getId(),
                        shopA.getName(),
                        shopAScores.asMap(),
                        shopAScores.totalIndex()
                ),
                new ShopComparisonResponse.ShopComparisonDetail(
                        shopB.getId(),
                        shopB.getName(),
                        shopBScores.asMap(),
                        shopBScores.totalIndex()
                ),
                toNarratives(aiResult)
        );
    }

    private boolean hasInsufficientDocuments(List<Document> shopADocuments, List<Document> shopBDocuments) {
        return shopADocuments == null || shopADocuments.isEmpty()
                || shopBDocuments == null || shopBDocuments.isEmpty();
    }

    private List<ShopComparisonResponse.ComparisonNarrative> toNarratives(AiShopComparisonResult aiResult) {
        if (aiResult == null || aiResult.narratives() == null || aiResult.narratives().isEmpty()) {
            return fallbackNarratives();
        }

        List<ShopComparisonResponse.ComparisonNarrative> narratives = aiResult.narratives().stream()
                .filter(Objects::nonNull)
                .filter(narrative -> recommendationShopReader.hasText(narrative.title())
                        && recommendationShopReader.hasText(narrative.body()))
                .map(narrative -> new ShopComparisonResponse.ComparisonNarrative(
                        narrative.title().trim(),
                        narrative.body().trim()
                ))
                .toList();

        if (narratives.isEmpty()) {
            return fallbackNarratives();
        }

        return narratives;
    }

    private List<ShopComparisonResponse.ComparisonNarrative> fallbackNarratives() {
        return List.of(new ShopComparisonResponse.ComparisonNarrative(
                "비교 정보 부족",
                "두 매장을 객관적으로 비교할 수 있는 검색 문서가 충분하지 않습니다. 리뷰나 매장 프로필 데이터가 쌓인 뒤 다시 비교해 주세요."
        ));
    }

    private String buildComparisonQuery(RamenShop shop, String focus) {
        if (recommendationShopReader.hasText(focus)) {
            return "%s %s".formatted(shop.getName(), focus);
        }

        return "%s 전반적인 맛 분위기 접근성 재방문 의사 메뉴 특징 리뷰".formatted(shop.getName());
    }

    private List<Document> collectComparisonDocuments(RamenShop shop, String focus) {
        String query = buildComparisonQuery(shop, focus);

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.eq(RetrievalMetadataKeys.SHOP_ID, String.valueOf(shop.getId())),
                builder.or(
                        builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.SHOP_PROFILE.name()),
                        builder.or(
                                builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.REVIEW_CHUNK.name()),
                                builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.EXTERNAL_REVIEW_CHUNK.name())
                        )
                )
        ).build();

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(8)
                        .similarityThreshold(0.3)
                        .filterExpression(filter)
                        .build()
        );
    }

    private String buildComparisonContext(RamenShop shop, List<Document> documents) {
        String documentContext = buildDocumentContext(documents);

        return """
            매장명: %s
            주소: %s
            태그: %s
            설명: %s

            [검색된 문서]
            %s
            """.formatted(
                shop.getName(),
                recommendationShopReader.addressTextOrDefault(shop),
                recommendationShopReader.tagsTextOrDefault(shop),
                recommendationShopReader.descriptionTextOrDefault(shop),
                documentContext
        );
    }

    private String buildDocumentContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "검색된 리뷰/프로필 문서가 부족합니다.";
        }

        return documents.stream()
                .map(this::formatDocument)
                .collect(Collectors.joining("\n"));
    }

    private String formatDocument(Document document) {
        Object documentType = document.getMetadata().get(RetrievalMetadataKeys.DOCUMENT_TYPE);
        Object source = document.getMetadata().get(RetrievalMetadataKeys.SOURCE);

        return """
            - 문서유형: %s
              출처: %s
              내용: %s
            """.formatted(
                documentType == null ? "UNKNOWN" : documentType,
                source == null ? "UNKNOWN" : source,
                document.getText()
        );
    }

    private AiShopComparisonResult generateComparisonResult(
            String focus,
            String contextA,
            String contextB
    ) {
        return chatClient.prompt()
                .user(user -> user.text(compareShopsTemplate)
                        .param("focus", recommendationShopReader.hasText(focus) ? focus : "전반적인 비교")
                        .param("contextA", contextA)
                        .param("contextB", contextB))
                .call()
                .entity(AiShopComparisonResult.class);
    }
}
