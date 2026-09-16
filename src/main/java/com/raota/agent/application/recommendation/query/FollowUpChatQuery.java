package com.raota.agent.application.recommendation.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Application-level input for a follow-up question over retrieved context. */
public record FollowUpChatQuery(String contextType, List<Long> shopIds, List<Message> messages) {

    public FollowUpChatQuery {
        shopIds = shopIds == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(shopIds));
        messages = messages == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public record Message(String role, String content) {
    }
}
