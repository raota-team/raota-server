package com.raota.agent.infrastructure.ai;

import com.raota.agent.application.mobile.port.MobileRecommendationReasonPort;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class SpringAiMobileRecommendationReasonAdapter implements MobileRecommendationReasonPort {

    private static final ParameterizedTypeReference<Map<String, String>> REASONS = new ParameterizedTypeReference<>() {
    };

    private final ChatClient chatClient;

    private final Resource reasonTemplate;

    /**
     * 공용 페르소나는 근거가 모자라면 "데이터가 부족하다"고 쓰게 해 추천 화면에 어울리지 않는다. 규칙은 프롬프트 한 곳에만 둔다.
     */
    public SpringAiMobileRecommendationReasonAdapter(ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/taste-recommendation-reason.st") Resource reasonTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.reasonTemplate = reasonTemplate;
    }

    @Override
    public Map<String, String> reasons(String conditions, String shopContext) {
        Map<String, String> reasons = chatClient.prompt()
            .user(user -> user.text(reasonTemplate).param("query", conditions).param("context", shopContext))
            .call()
            .entity(REASONS);
        if (reasons == null) {
            return Map.of();
        }
        return reasons;
    }

}
