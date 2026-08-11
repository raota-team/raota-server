package com.raota.community.domain.event;

public record PostIndexingEvent(
        Long postId,
        PostIndexingAction action
) {

    public PostIndexingEvent {
        if (action == null) {
            action = PostIndexingAction.UPSERT;
        }
    }

    public static PostIndexingEvent upsert(Long postId) {
        return new PostIndexingEvent(postId, PostIndexingAction.UPSERT);
    }

    public static PostIndexingEvent delete(Long postId) {
        return new PostIndexingEvent(postId, PostIndexingAction.DELETE);
    }
}
