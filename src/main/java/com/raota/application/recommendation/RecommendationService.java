package com.raota.application.recommendation;
import com.raota.presentation.api.recommendation.request.*;
import com.raota.presentation.api.recommendation.response.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RecommendationService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, @Value("classpath:/prompts/system-persona.st") Resource systemResource) {
        this.chatClient = chatClientBuilder
                .defaultSystem(systemResource)
                .build();
        this.vectorStore = vectorStore;
    }

    public TasteRecommendationResponse recommendByTaste(TasteRecommendationRequest request) {
        return null;
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
