package com.raota.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/** 운영처럼 v1 리프레시 토큰을 Redis에 저장해도 v1·v2 저장소가 함께 뜨는지 확인한다. */
@TestPropertySource(properties = "app.auth.refresh-token.store-type=redis")
class RedisRefreshTokenStoreContextTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void 운영과_같은_Redis_저장소_설정으로_v1과_v2_리프레시_토큰_저장소가_함께_뜬다() {
        assertThat(context.getBean(com.raota.web.account.infrastructure.persistence.auth.RefreshTokenStore.class))
            .isInstanceOf(com.raota.web.account.infrastructure.persistence.auth.RedisRefreshTokenStore.class);
        assertThat(context.getBean(com.raota.mobile.account.application.port.RefreshTokenStore.class))
            .isInstanceOf(com.raota.mobile.account.infrastructure.auth.MobileRedisRefreshTokenStore.class);
    }

}
