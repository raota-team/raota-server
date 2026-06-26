package com.raota.infrastructure.ai;

import com.raota.application.ramenShop.port.RamenShopComparisonNarrativePort;
import com.raota.application.ramenShop.result.AiRamenShopComparisonResult;
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
        return chatClient.prompt()
                .user(user -> user.text(compareShopsTemplate)
                        .param("focus", focus)
                        .param("contextA", contextA)
                        .param("contextB", contextB))
                .call()
                .entity(AiRamenShopComparisonResult.class);
    }
}
