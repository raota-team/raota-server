package com.raota.domain.retrieval.event;

import com.raota.global.messaging.MessagePublisher;
import com.raota.global.messaging.MessagingTopics;
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
