package com.raota.mobile.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class MobileApiPathTest {

    @Test
    void v2_루트와_하위_경로를_v2_요청으로_본다() {
        assertThat(MobileApiPath.matches(request("/api/v2"))).isTrue();
        assertThat(MobileApiPath.matches(request("/api/v2/"))).isTrue();
        assertThat(MobileApiPath.matches(request("/api/v2/members/me"))).isTrue();
    }

    @Test
    void 인코딩된_경로도_디코딩한_경로로_판단한다() {
        assertThat(MobileApiPath.matches(request("/%61pi/v2/members/me"))).isTrue();
        assertThat(MobileApiPath.matches(request("/api/%76%32/members/me"))).isTrue();
    }

    @Test
    void 접두사만_같은_경로와_v1_경로는_v2_요청이_아니다() {
        assertThat(MobileApiPath.matches(request("/api/v2x/members"))).isFalse();
        assertThat(MobileApiPath.matches(request("/api/v1/community/posts"))).isFalse();
        assertThat(MobileApiPath.matches(request("/users/me/profile"))).isFalse();
    }

    private static MockHttpServletRequest request(String requestUri) {
        return new MockHttpServletRequest("GET", requestUri);
    }

}
