package com.raota.agent.infrastructure.ai;

import com.raota.agent.application.mobile.port.MobileRecommendationReasonPort;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SpringAiMobileRecommendationReasonAdapter implements MobileRecommendationReasonPort {

    private static final ParameterizedTypeReference<Map<String, String>> REASONS = new ParameterizedTypeReference<>() {
    };

    private final ChatClient chatClient;

    private final Resource reasonTemplate;

    private final String reasoningEffort;

    private final int maxCompletionTokens;

    /**
     * 공용 페르소나는 근거가 모자라면 "데이터가 부족하다"고 쓰게 해 추천 화면에 어울리지 않는다. 규칙은 프롬프트 한 곳에만 둔다.
     *
     * <p>
     * 매장 6곳에 2~3문장씩 쓰면 gpt-oss는 생각하는 토큰까지 합쳐 기본 출력 상한(3072)을 넘겨 JSON이 잘리고, 이유가 통째로 빠진다. 이
     * 일은 깊이 생각할 필요가 없어 생각 정도를 낮추고(짧고 빨라진다) 출력 상한을 넉넉히 둔다. 생각 정도를 받지 않는 모델이면 빈 값으로 끈다.
     * </p>
     */
    public SpringAiMobileRecommendationReasonAdapter(ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/taste-recommendation-reason.st") Resource reasonTemplate,
            @Value("${app.mobile.recommendation.reason.reasoning-effort:low}") String reasoningEffort,
            @Value("${app.mobile.recommendation.reason.max-completion-tokens:6000}") int maxCompletionTokens) {
        this.chatClient = chatClientBuilder.build();
        this.reasonTemplate = reasonTemplate;
        this.reasoningEffort = reasoningEffort;
        this.maxCompletionTokens = maxCompletionTokens;
    }

    /** 빌더는 값을 바꿔 가며 쓰는 객체라 동시에 들어온 추천끼리 나누지 않도록 부를 때마다 새로 만든다. */
    private OpenAiChatOptions.Builder options() {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder().maxCompletionTokens(maxCompletionTokens);
        if (StringUtils.hasText(reasoningEffort)) {
            builder.reasoningEffort(reasoningEffort);
        }
        return builder;
    }

    @Override
    public Map<String, String> reasons(String conditions, String shopContext) {
        Map<String, String> reasons = chatClient.prompt()
            .options(options())
            .user(user -> user.text(reasonTemplate).param("query", conditions).param("context", shopContext))
            .call()
            .entity(REASONS);
        if (reasons == null) {
            return Map.of();
        }
        return reasons;
    }

}
