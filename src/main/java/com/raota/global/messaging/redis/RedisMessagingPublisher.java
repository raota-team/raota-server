package com.raota.global.messaging.redis;

import com.raota.global.messaging.MessagePublisher;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessagingPublisher implements MessagePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper redisObjectMapper;

    @Override
    public void publish(String topic, Object payload) {
        String jsonPayload  = redisObjectMapper.writeValueAsString(payload);
        redisTemplate.opsForStream().add(topic, Collections.singletonMap("payload",jsonPayload));
        redisTemplate.opsForStream().trim(topic,1000);
    }
}
