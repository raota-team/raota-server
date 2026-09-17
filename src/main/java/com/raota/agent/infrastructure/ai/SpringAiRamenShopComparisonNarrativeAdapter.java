package com.raota.agent.infrastructure.ai;

import com.raota.agent.application.ramenshop.port.RamenShopComparisonNarrativePort;
import com.raota.agent.application.ramenshop.result.AiRamenShopComparisonResult;
import com.raota.global.presentation.common.AiResponseFormatException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class SpringAiRamenShopComparisonNarrativeAdapter implements RamenShopComparisonNarrativePort {

    private final ChatClient chatClient;
    private final Resource compareShopsTemplate;

    public SpringAiRamenShopComparisonNarrativeAdapter(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/system-persona.st") Resource systemPersona,
            @Value("classpath:/prompts/compare-shops.st") Resource compareShopsTemplate
    ) {
        this.chatClient = chatClientBuilder.defaultSystem(systemPersona).build();
        this.compareShopsTemplate = compareShopsTemplate;
    }

    @Override
    public AiRamenShopComparisonResult generateComparisonNarratives(
            String focus,
            String contextA,
            String contextB
    ) {
        try {
            return chatClient.prompt()
                    .user(user -> user.text(compareShopsTemplate)
                            .param("focus", focus)
                            .param("contextA", contextA)
                            .param("contextB", contextB))
                    .call()
                    // The provider can still return truncated JSON even when the prompt asks
                    // for JSON. Schema validation lets Spring AI retry with concrete parser
                    // feedback before the request is recorded as an execution error.
                    .entity(AiRamenShopComparisonResult.class, spec -> spec.validateSchema());
        } catch (RuntimeException exception) {
            throw new AiResponseFormatException("AI 매장 비교 응답 형식이 올바르지 않습니다.", exception);
        }
    }
}
