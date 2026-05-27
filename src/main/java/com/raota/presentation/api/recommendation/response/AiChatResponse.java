package com.raota.presentation.api.recommendation.response;

public record AiChatResponse(ChatMessageResponse message) {
    public record ChatMessageResponse(String role, String content) {}
}
