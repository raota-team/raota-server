package com.raota.agent.presentation.recommendation.request;
import java.util.List;
public record AiChatRequest(String contextType, List<Long> shopIds, List<ChatMessage> messages) {
    public record ChatMessage(String role, String content) {}
}
