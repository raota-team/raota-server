package com.raota.agent.application.evaluation;

public record RagExpectedShop(Long shopId, int relevance) {

    public RagExpectedShop {
        if (shopId == null || shopId < 1) {
            throw new IllegalArgumentException("정답 매장 ID는 양수여야 합니다.");
        }
        if (relevance < 1 || relevance > 3) {
            throw new IllegalArgumentException("매장 관련성 등급은 1~3이어야 합니다.");
        }
    }
}
