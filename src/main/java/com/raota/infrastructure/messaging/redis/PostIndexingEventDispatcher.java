package com.raota.infrastructure.messaging.redis;

import com.raota.domain.retrieval.event.PostIndexingEvent;
import com.raota.infrastructure.messaging.MessagePublisher;
import com.raota.infrastructure.messaging.MessagingTopics;
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
