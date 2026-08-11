package com.raota.unit.infrastructure.messaging.redis;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raota.community.domain.event.PostIndexingEvent;
import com.raota.agent.infrastructure.messaging.redis.PostIndexingStreamListener;
import com.raota.agent.application.retrieval.RetrievalIndexingService;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PostIndexingStreamListenerTest {

    @Mock
    private RetrievalIndexingService retrievalIndexingService;

    @Mock
    private ObjectMapper redisObjectMapper;

    @InjectMocks
    private PostIndexingStreamListener listener;

    @Test
    @DisplayName("Redis 스트림 메시지를 수신하면 역직렬화하여 인덱싱 서비스를 호출한다.")
    void on_message_invokes_indexing_service() throws Exception {
        // given
        String jsonPayload = "{\"postId\": 1}";
        PostIndexingEvent event = PostIndexingEvent.upsert(1L);

        MapRecord<String, String, String> mockRecord = MapRecord.create("stream:key", Collections.singletonMap("payload", jsonPayload));

        when(redisObjectMapper.readValue(jsonPayload, PostIndexingEvent.class)).thenReturn(event);

        // when
        listener.onMessage(mockRecord);

        // then
        verify(retrievalIndexingService).indexPost(1L);
    }

    @Test
    @DisplayName("삭제 이벤트를 수신하면 벡터 문서 삭제 서비스를 호출한다.")
    void on_delete_message_invokes_delete_service() throws Exception {
        // given
        String jsonPayload = "{\"postId\": 1, \"action\": \"DELETE\"}";
        PostIndexingEvent event = PostIndexingEvent.delete(1L);

        MapRecord<String, String, String> mockRecord =
                MapRecord.create("stream:key", Collections.singletonMap("payload", jsonPayload));

        when(redisObjectMapper.readValue(jsonPayload, PostIndexingEvent.class)).thenReturn(event);

        // when
        listener.onMessage(mockRecord);

        // then
        verify(retrievalIndexingService).deletePost(1L);
    }
}
