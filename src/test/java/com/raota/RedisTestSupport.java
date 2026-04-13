package com.raota;


import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;

/**
 * 모든 Redis 관련 테스트에서 상속받아 사용할 베이스 클래스.
 */
public abstract class RedisTestSupport {

    private static final String REDIS_IMAGE = "redis:7.2-alpine";
    protected static final RedisContainer REDIS_CONTAINER;

    static {
        // 1. Redis 컨테이너 정의 (무작위 포트 사용)
        REDIS_CONTAINER = new RedisContainer(DockerImageName.parse(REDIS_IMAGE));
        
        // 2. 컨테이너 시작
        REDIS_CONTAINER.start();
    }

    /**
     * 컨테이너가 뜬 후, 무작위로 할당된 포트 번호를 
     * 스프링의 'spring.data.redis.port' 설정에 동적으로 주입한다.
     */
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }
}
