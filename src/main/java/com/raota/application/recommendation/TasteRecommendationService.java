package com.raota.application.recommendation;

import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.presentation.api.recommendation.request.TasteRecommendationRequest;
import com.raota.presentation.api.recommendation.response.TasteRecommendationResponse;
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
    private final RamenShopRepository ramenShopRepository;
    private final Resource recommendationReasonTemplate;

    public TasteRecommendationService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            RamenShopRepository ramenShopRepository,
            @Value("classpath:/prompts/system-persona.st") Resource systemPersona,
            @Value("classpath:/prompts/taste-recommendation-reason.st") Resource recommendationReasonTemplate
    ) {
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPersona)
                .build();
        this.vectorStore = vectorStore;
        this.ramenShopRepository = ramenShopRepository;
        this.recommendationReasonTemplate = recommendationReasonTemplate;
    }

    public TasteRecommendationResponse recommendByTaste(TasteRecommendationRequest request) {
        validateTasteRecommendationRequest(request);

        String query = buildTasteQuery(request);
        List<Document> searchResult = searchRecommendedShops(query);
        String context = buildRecommendationContext(searchResult);
        Map<String, String> aiReasons = generateRecommendationReasons(query, context);

        return buildTasteRecommendationResponse(searchResult, aiReasons);
    }

    private void validateTasteRecommendationRequest(TasteRecommendationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("취향 추천 요청은 필수입니다.");
        }

        if (!hasText(request.soup())) {
            throw new IllegalArgumentException("국물 취향은 필수입니다.");
        }

        if (!hasText(request.mood())) {
            throw new IllegalArgumentException("상황/분위기는 필수입니다.");
        }

        if (!hasText(request.priority())) {
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

        if (hasText(request.freeText())) {
            queryBuilder.append(" ").append(request.freeText().trim());
        }

        return queryBuilder.toString();
    }

    private List<Document> searchRecommendedShops(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(4)
                        .similarityThreshold(0.7)
                        .build()
        );
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
        Long shopId = Long.valueOf(document.getMetadata().get("shopId").toString());
        RamenShop shop = getRamenShop(shopId);

        return new TasteRecommendationResponse.RecommendedShopResponse(
                shop.getId(),
                shop.getName(),
                getPrimaryTag(shop),
                shop.getAddress().fullAddress(),
                aiReasons.getOrDefault(String.valueOf(shopId), "취향에 맞는 추천 매장입니다."),
                shop.getImageUrl(),
                (int) (document.getScore() * 100),
                false
        );
    }

    private RamenShop getRamenShop(Long shopId) {
        return ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("라멘샵을 찾을 수 없습니다. id=" + shopId));
    }

    private String getPrimaryTag(RamenShop shop) {
        if (shop.getTags() == null || shop.getTags().isEmpty()) {
            return "";
        }
        return shop.getTags().getFirst();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
