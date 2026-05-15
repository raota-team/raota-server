package com.raota.domain.retrieval;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raota.domain.retrieval.event.PostIndexingEvent;
import com.raota.domain.retrieval.messaging.PostIndexingStreamListener;
import com.raota.domain.retrieval.service.RetrievalIndexingService;
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
        PostIndexingEvent event = new PostIndexingEvent(1L);

        MapRecord<String, String, String> mockRecord = MapRecord.create("stream:key", Collections.singletonMap("payload", jsonPayload));

        when(redisObjectMapper.readValue(jsonPayload, PostIndexingEvent.class)).thenReturn(event);

        // when
        listener.onMessage(mockRecord);

        // then
        verify(retrievalIndexingService).indexPost(1L);
    }
}
