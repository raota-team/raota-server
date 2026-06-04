package com.raota.infrastructure.redis;

import com.raota.infrastructure.messaging.redis.PostIndexingStreamListener;
import com.raota.infrastructure.messaging.MessagingTopics;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final PostIndexingStreamListener postIndexingStreamListener;
    private final RedisStreamErrorHandler redisStreamErrorHandler;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CONSUMER_GROUP = "raota-retrieval-group";
    private static final String CONSUMER_NAME = "instance-1"; // 서버 스케일아웃 시 UUID 등으로 유니크하게 변경 가능

    @Bean
    public Subscription postIndexingSubscription() {
        initConsumerGroup(MessagingTopics.POST_INDEXING, CONSUMER_GROUP);

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .errorHandler(redisStreamErrorHandler)
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        Subscription subscription = listenerContainer.receive(
                Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                StreamOffset.create(MessagingTopics.POST_INDEXING, ReadOffset.lastConsumed()),
                postIndexingStreamListener
        );

        log.info("Redis Stream listener started. streamKey={}, consumerGroup={}, consumerName={}, pollTimeoutSeconds={}",
                MessagingTopics.POST_INDEXING, CONSUMER_GROUP, CONSUMER_NAME, 1);
        listenerContainer.start();
        return subscription;
    }

    private void initConsumerGroup(String streamKey, String groupName) {
        try {
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(streamKey))) {
                stringRedisTemplate.opsForStream().createGroup(streamKey, groupName);
                log.info("Redis Stream 생성 및 Consumer Group 초기화 완료: {}", streamKey);
                return;
            }

            stringRedisTemplate.opsForStream().createGroup(streamKey, groupName);
            log.info("Consumer Group 초기화 완료: {}", groupName);
        } catch (Exception e) {
            if (isBusyGroupException(e)) {
                log.info("Consumer Group이 이미 존재합니다: {}", groupName);
            } else {
                log.error("Redis Stream Consumer Group 초기화 중 에러 발생", e);
            }
        }
    }

    private boolean isBusyGroupException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
