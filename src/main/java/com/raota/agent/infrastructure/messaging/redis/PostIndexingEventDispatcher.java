package com.raota.agent.infrastructure.messaging.redis;

import com.raota.community.domain.event.PostIndexingEvent;
import com.raota.global.messaging.MessagePublisher;
import com.raota.agent.infrastructure.messaging.MessagingTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PostIndexingEventDispatcher {

    private final MessagePublisher messagePublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchToRedisStream(PostIndexingEvent event){
        messagePublisher.publish(MessagingTopics.POST_INDEXING,event);
    }
}
