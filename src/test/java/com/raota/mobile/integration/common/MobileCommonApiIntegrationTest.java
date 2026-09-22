package com.raota.mobile.integration.common;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.support.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class MobileCommonApiIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("ping은 로그인 없이 v2 성공 형식으로 응답한다.")
    void pingReturnsV2SuccessShape() {
        given()
                .when().get("/api/v2/ping")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true))
                .body("data.status", is("ok"))
                .body("data.serverTime", notNullValue())
                .body("error", nullValue())
                .body("meta.requestId", notNullValue());
    }

    @Test
    @DisplayName("응답 헤더와 meta의 requestId가 같고 요청마다 새로 만들어진다.")
    void requestIdIsSharedByHeaderAndBody() {
        Response first = given().when().get("/api/v2/ping").andReturn();
        Response second = given().when().get("/api/v2/ping").andReturn();

        String headerRequestId = first.getHeader(RequestIdFilter.HEADER_NAME);

        assertThat(headerRequestId).isNotBlank();
        assertThat(first.jsonPath().getString("meta.requestId")).isEqualTo(headerRequestId);
        assertThat(second.getHeader(RequestIdFilter.HEADER_NAME)).isNotEqualTo(headerRequestId);
    }

    @Test
    @DisplayName("인가 규칙이 없는 v2 경로는 열리지 않고 v2 형식으로 막힌다.")
    void unmappedV2PathIsClosedByDefault() {
        given()
                .when().get("/api/v2/not-registered-yet")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("success", is(false))
                .body("error.code", is("UNAUTHORIZED"))
                .body("meta.requestId", notNullValue());
    }

    @Test
    @DisplayName("v1 토큰 형식이 아닌 Authorization 헤더를 보내도 v2 공개 경로는 500이 되지 않는다.")
    void v1FilterDoesNotRunOnV2Paths() {
        given()
                .header("Authorization", "Bearer not-a-v1-token")
                .when().get("/api/v2/ping")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true));
    }

    @Test
    @DisplayName("v1 응답 형식은 그대로 유지된다.")
    void v1ResponseShapeIsUnchanged() {
        given()
                .when().get("/api/v1/community/posts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", is("SUCCESS"))
                .body("success", is(true));
    }
}
