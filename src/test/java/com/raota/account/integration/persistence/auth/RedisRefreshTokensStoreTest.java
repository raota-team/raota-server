package com.raota.account.integration.persistence.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.account.infrastructure.auth.AuthRedisProperties;
import com.raota.account.infrastructure.persistence.auth.RedisRefreshTokenStore;
import com.raota.account.infrastructure.persistence.auth.RefreshTokenStore;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;



public class RedisRefreshTokensStoreTest extends BaseIntegrationTest {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:token:";
    private static final String REFRESH_MEMBER_KEY_PREFIX = "auth:refresh:member:";

    private RefreshTokenStore refreshTokenStore;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp(){
        String host = REDIS_CONTAINER.getHost();
        int port = REDIS_CONTAINER.getMappedPort(6379);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        this.redisTemplate = new StringRedisTemplate(factory);
        this.refreshTokenStore = new RedisRefreshTokenStore(
                this.redisTemplate,
                new AuthRedisProperties(REFRESH_TOKEN_KEY_PREFIX, REFRESH_MEMBER_KEY_PREFIX)
        );
    }

    @Test
    void 레디스의_리프레쉬토큰_저장을_확인한다(){
        Long memberId = 1L;
        String testToken = "sample-token";
        Instant testExpiry = Instant.now().plusSeconds(3600);

        refreshTokenStore.save(memberId,testToken,testExpiry);

        String raw = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY_PREFIX + testToken);
        assertThat(raw).isNotNull();
        assertThat(raw).contains(String.valueOf(memberId));
    }
}
