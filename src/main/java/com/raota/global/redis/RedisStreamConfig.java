package com.raota.global.redis;

import com.raota.domain.retrieval.messaging.PostIndexingStreamListener;
import com.raota.global.messaging.MessagingTopics;
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
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CONSUMER_GROUP = "raota-retrieval-group";
    private static final String CONSUMER_NAME = "instance-1"; // 서버 스케일아웃 시 UUID 등으로 유니크하게 변경 가능

    @Bean
    public Subscription postIndexingSubscription() {
        initConsumerGroup(MessagingTopics.POST_INDEXING, CONSUMER_GROUP);

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        Subscription subscription = listenerContainer.receive(
                Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                StreamOffset.create(MessagingTopics.POST_INDEXING, ReadOffset.lastConsumed()),
                postIndexingStreamListener
        );

        listenerContainer.start();
        return subscription;
    }

    private void initConsumerGroup(String streamKey, String groupName) {
        try {
            // 스트림 키가 없으면 에러가 나므로 미리 존재 여부 체크
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(streamKey))) {
                stringRedisTemplate.opsForStream().createGroup(streamKey, groupName);
                log.info("Redis Stream 생성 및 Consumer Group 초기화 완료: {}", streamKey);
            } else {
                // 스트림은 존재하나 그룹이 없을 경우 그룹만 생성
                stringRedisTemplate.opsForStream().createGroup(streamKey, groupName);
                log.info("Consumer Group 초기화 완료: {}", groupName);
            }
        } catch (Exception e) {
            // 이미 그룹이 존재하는 경우 발생하는 에러는 무시
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("Consumer Group이 이미 존재합니다: {}", groupName);
            } else {
                log.error("Redis Stream Consumer Group 초기화 중 에러 발생", e);
            }
        }
    }
}