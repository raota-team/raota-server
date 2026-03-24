package com.raota.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.domain.auth.store.RedisRefreshTokenStore;
import com.raota.domain.auth.store.RefreshTokenStore;
import com.raota.RedisTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class RedisRefreshTokensStoreTest extends RedisTestSupport {

    private RefreshTokenStore refreshTokenStore;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp(){
        String host = REDIS_CONTAINER.getRedisHost();
        int port = REDIS_CONTAINER.getMappedPort(6379);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        this.redisTemplate = new StringRedisTemplate(factory);
        this.refreshTokenStore = new RedisRefreshTokenStore(this.redisTemplate);
    }

    @Test
    void 레디스의_리프레쉬토큰_저장을_확인한다(){
        Long memberId = 1L;
        String testToken = "sample-token";
        Instant testExpiry = Instant.now().plusSeconds(3600);

        refreshTokenStore.save(memberId,testToken,testExpiry);

        String raw = redisTemplate.opsForValue().get("auth:refresh:token:" + testToken);
        assertThat(raw).isNotNull();
        assertThat(raw).contains(String.valueOf(memberId));
    }
}
