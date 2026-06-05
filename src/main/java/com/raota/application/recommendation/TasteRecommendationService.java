package com.raota.application.recommendation;

import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.presentation.api.recommendation.request.TasteRecommendationRequest;
import com.raota.presentation.api.recommendation.response.TasteRecommendationResponse;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class TasteRecommendationService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final RecommendationShopReader recommendationShopReader;
    private final Resource recommendationReasonTemplate;

    public TasteRecommendationService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            RecommendationShopReader recommendationShopReader,
            @Value("classpath:/prompts/system-persona.st") Resource systemPersona,
            @Value("classpath:/prompts/taste-recommendation-reason.st") Resource recommendationReasonTemplate
    ) {
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPersona)
                .build();
        this.vectorStore = vectorStore;
        this.recommendationShopReader = recommendationShopReader;
        this.recommendationReasonTemplate = recommendationReasonTemplate;
    }

    public TasteRecommendationResponse recommendByTaste(TasteRecommendationRequest request) {
        validateTasteRecommendationRequest(request);

        // 사용자 취향을 하나의 검색 질의로 합쳐 벡터 검색에 사용한다.
        String query = buildTasteQuery(request);
        List<Document> searchResult = searchRecommendedShops(query);
        // 검색된 문서를 LLM 프롬프트용 문맥으로 변환해 매장별 추천 사유를 생성한다.
        String context = buildRecommendationContext(searchResult);
        Map<String, String> aiReasons = generateRecommendationReasons(query, context);

        return buildTasteRecommendationResponse(searchResult, aiReasons);
    }

    private void validateTasteRecommendationRequest(TasteRecommendationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("취향 추천 요청은 필수입니다.");
        }

        if (!recommendationShopReader.hasText(request.soup())) {
            throw new IllegalArgumentException("국물 취향은 필수입니다.");
        }

        if (!recommendationShopReader.hasText(request.mood())) {
            throw new IllegalArgumentException("상황/분위기는 필수입니다.");
        }

        if (!recommendationShopReader.hasText(request.priority())) {
            throw new IllegalArgumentException("우선순위는 필수입니다.");
        }
    }

    private String buildTasteQuery(TasteRecommendationRequest request) {
        StringBuilder queryBuilder = new StringBuilder()
                .append(request.soup().trim())
                .append(" ")
                .append(request.mood().trim())
                .append(" ")
                .append(request.priority().trim());

        if (recommendationShopReader.hasText(request.freeText())) {
            queryBuilder.append(" ").append(request.freeText().trim());
        }

        return queryBuilder.toString();
    }

    private List<Document> searchRecommendedShops(String query) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(8)
                        .similarityThreshold(0.35)
                        .build()
        );

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        // 같은 매장이 여러 문서로 검색될 수 있어, 점수가 가장 높은 문서만 대표로 남긴다.
        Map<Long, Document> bestByShop = new LinkedHashMap<>();
        for (Document document : documents) {
            Long shopId = parseShopId(document.getMetadata().get("shopId"));
            Document current = bestByShop.get(shopId);
            if (current == null || document.getScore() > current.getScore()) {
                bestByShop.put(shopId, document);
            }
        }

        return bestByShop.values().stream()
                .sorted(Comparator.comparingDouble(Document::getScore).reversed())
                .limit(4)
                .toList();
    }

    private String buildRecommendationContext(List<Document> searchResult) {
        return searchResult.stream()
                .map(doc -> String.format("ID: %s, 내용: %s",
                        doc.getMetadata().get("shopId"), doc.getText()))
                .collect(Collectors.joining("\n"));
    }

    private Map<String, String> generateRecommendationReasons(String query, String context) {
        return chatClient.prompt()
                .user(user -> user.text(recommendationReasonTemplate)
                        .param("query", query)
                        .param("context", context))
                .call()
                .entity(new ParameterizedTypeReference<Map<String, String>>() {
                });
    }

    private TasteRecommendationResponse buildTasteRecommendationResponse(
            List<Document> searchResult,
            Map<String, String> aiReasons
    ) {
        List<TasteRecommendationResponse.RecommendedShopResponse> recommendedShops = searchResult.stream()
                .map(document -> toRecommendedShopResponse(document, aiReasons))
                .toList();

        return new TasteRecommendationResponse(recommendedShops);
    }

    private TasteRecommendationResponse.RecommendedShopResponse toRecommendedShopResponse(
            Document document,
            Map<String, String> aiReasons
    ) {
        Long shopId = parseShopId(document.getMetadata().get("shopId"));
        RamenShop shop = recommendationShopReader.getRamenShop(shopId);

        return new TasteRecommendationResponse.RecommendedShopResponse(
                shop.getId(),
                shop.getName(),
                recommendationShopReader.primaryTag(shop),
                recommendationShopReader.addressText(shop),
                aiReasons.getOrDefault(String.valueOf(shopId), "취향에 맞는 추천 매장입니다."),
                shop.getImageUrl(),
                (int) (document.getScore() * 100),
                false
        );
    }

    private Long parseShopId(Object rawShopId) {
        if (rawShopId == null) {
            throw new IllegalArgumentException("추천 결과에 shopId 메타데이터가 없습니다.");
        }

        String value = rawShopId.toString().trim().replace("\"", "");
        return Long.valueOf(value);
    }
}
