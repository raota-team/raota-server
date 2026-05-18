package com.raota.application.recommendation;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.presentation.api.recommendation.request.*;
import com.raota.presentation.api.recommendation.response.*;
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
public class RecommendationService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final RamenShopRepository ramenShopRepository;
    private final Resource recommendationReasonTemplate;

    public RecommendationService(
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
        String query =  String.format("%s %s %s", request.soup(), request.mood(), request.priority());

        List<Document> searchResult = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(4)
                        .similarityThreshold(0.7)
                        .build()
        );

        String context = searchResult.stream()
                .map(doc -> String.format("ID: %s, 내용: %s",
                        doc.getMetadata().get("shopId"), doc.getText()))
                .collect(Collectors.joining("\n"));

        Map<String,String> aiReasons = chatClient.prompt()
                .user(u->u.text(recommendationReasonTemplate)
                        .param("query",query)
                        .param("context",context))
                .call()
                .entity(new ParameterizedTypeReference<Map<String, String>>() {});

        List<TasteRecommendationResponse.RecommendedShopResponse> recommendedShops = searchResult.stream()
                .map(doc ->{
                    Long shopId = Long.valueOf(doc.getMetadata().get("shopId").toString());
                    RamenShop shop = ramenShopRepository.findById(shopId)
                            .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
                    return new TasteRecommendationResponse.RecommendedShopResponse(
                            shop.getId(),
                            shop.getName(),
                            shop.getTags().get(0),
                            shop.getAddress().fullAddress(),
                            aiReasons.getOrDefault(String.valueOf(shopId), "취향에 맞는 추천 매장입니다."),
                            shop.getImageUrl(),
                            (int)(doc.getScore()*100),
                            false
                    );
                }).toList();

        return new TasteRecommendationResponse(recommendedShops);
    }

    public ShopComparisonResponse compareShops(ShopComparisonRequest request) {
        return null;
    }

    public ReviewSummaryResponse summarizeReviews(ReviewSummaryRequest request) {
        return null;
    }

    public AiChatResponse followUpChat(AiChatRequest request) {
        return null;
    }
}
