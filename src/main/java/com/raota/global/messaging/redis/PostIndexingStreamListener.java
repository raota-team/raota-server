package com.raota.global.messaging.redis;


import com.raota.domain.retrieval.event.PostIndexingEvent;
import com.raota.application.retrieval.RetrievalIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostIndexingStreamListener implements StreamListener<String, MapRecord<String,String,String>> {

    private final RetrievalIndexingService retrievalIndexingService;
    private final ObjectMapper redisObjectmapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        long startedAt = System.currentTimeMillis();
        String messageId = message.getId().getValue();
        String jsonPayload = message.getValue().get("payload");
        PostIndexingEvent event = redisObjectmapper.readValue(jsonPayload, PostIndexingEvent.class);

        try {
            log.info("비동기 게시글 인덱싱 시작: messageId={}, postId={}, action={}",
                    messageId, event.postId(), event.action());
            switch (event.action()) {
                case UPSERT -> retrievalIndexingService.indexPost(event.postId());
                case DELETE -> retrievalIndexingService.deletePost(event.postId());
            }
            log.info("비동기 게시글 인덱싱 완료: messageId={}, postId={}, action={}, durationMs={}",
                    messageId, event.postId(), event.action(), System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            log.error("비동기 게시글 인덱싱 실패: messageId={}, postId={}, action={}, durationMs={}, payload={}",
                    messageId, event.postId(), event.action(), System.currentTimeMillis() - startedAt, jsonPayload, e);
            throw e;
        }
    }
}
