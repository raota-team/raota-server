package com.raota.infrastructure.redis;

import com.raota.infrastructure.messaging.redis.PostIndexingStreamListener;
import com.raota.infrastructure.messaging.MessagingTopics;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.redis.stream.consumer-name:${HOSTNAME:instance-1}}")
    private String consumerName;

    @Value("${app.redis.stream.poll-timeout-seconds:10}")
    private long pollTimeoutSeconds;

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> postIndexingStreamListenerContainer() {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(pollTimeoutSeconds))
                        .errorHandler(redisStreamErrorHandler)
                        .build();

        return StreamMessageListenerContainer.create(redisConnectionFactory, options);
    }

    @Bean
    public Subscription postIndexingSubscription(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> postIndexingStreamListenerContainer
    ) {
        initConsumerGroup(MessagingTopics.POST_INDEXING, CONSUMER_GROUP);
        Subscription subscription = postIndexingStreamListenerContainer.receive(
                Consumer.from(CONSUMER_GROUP, consumerName),
                StreamOffset.create(MessagingTopics.POST_INDEXING, ReadOffset.lastConsumed()),
                postIndexingStreamListener
        );

        log.info("Redis Stream listener started. streamKey={}, consumerGroup={}, consumerName={}, pollTimeoutSeconds={}",
                MessagingTopics.POST_INDEXING, CONSUMER_GROUP, consumerName, pollTimeoutSeconds);
        postIndexingStreamListenerContainer.start();
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
