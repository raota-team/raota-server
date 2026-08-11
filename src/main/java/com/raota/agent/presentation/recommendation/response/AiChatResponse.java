package com.raota.agent.presentation.recommendation.response;

public record AiChatResponse(ChatMessageResponse message) {
    public record ChatMessageResponse(String role, String content) {}
}
