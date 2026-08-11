package com.raota.agent.infrastructure.messaging.redis;

import static org.mockito.Mockito.verify;

import com.raota.community.domain.event.PostIndexingEvent;
import com.raota.agent.infrastructure.messaging.redis.PostIndexingEventDispatcher;
import com.raota.global.messaging.MessagePublisher;
import com.raota.agent.infrastructure.messaging.MessagingTopics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostIndexingEventDispatcherTest {

    @Mock
    private MessagePublisher messagePublisher;

    @InjectMocks
    private PostIndexingEventDispatcher dispatcher;

    @Test
    @DisplayName("이벤트를 전달받으면 Redis Stream 토픽으로 퍼블리싱한다.")
    void dispatch_to_redis_stream_success() {
        // given
        PostIndexingEvent event = PostIndexingEvent.upsert(1L);

        // when
        dispatcher.dispatchToRedisStream(event);

        // then
        verify(messagePublisher).publish(MessagingTopics.POST_INDEXING, event);
    }
}
