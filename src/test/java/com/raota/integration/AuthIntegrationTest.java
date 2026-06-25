package com.raota.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;

import com.raota.domain.auth.model.RefreshToken;
import com.raota.domain.auth.repository.RefreshTokenRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.support.BaseIntegrationTest;
import io.restassured.RestAssured;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class AuthIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        refreshTokenRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("유효한 Refresh Token 쿠키를 보내면 새로운 Access Token이 발급된다.")
    void refresh_token_success() {
        MemberProfile member = memberRepository.save(MemberProfile.builder()
                .nickname("testuser")
                .build());

        String refreshTokenValue = "valid-refresh-token";
        refreshTokenRepository.save(RefreshToken.builder()
                .memberId(member.getId())
                .token(refreshTokenValue)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build());

        given()
                .cookie("raota_refresh_token", refreshTokenValue)
        .when()
                .post("/auth/refresh")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.accessToken", notNullValue());
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token으로 요청 시 401 에러를 반환한다.")
    void refresh_token_fail_with_invalid_token() {
        given()
                .cookie("raota_refresh_token", "invalid-token")
        .when()
                .post("/auth/refresh")
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("탈퇴한 회원의 Refresh Token으로 요청 시 401 에러와 재가입 안내를 반환한다.")
    void refresh_token_fail_for_withdrawn_member() {
        MemberProfile member = memberRepository.save(MemberProfile.builder()
                .nickname("withdrawn-user")
                .build());
        member.softDelete(java.time.LocalDateTime.now());
        memberRepository.save(member);

        String refreshTokenValue = "withdrawn-refresh-token";
        refreshTokenRepository.save(RefreshToken.builder()
                .memberId(member.getId())
                .token(refreshTokenValue)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build());

        given()
                .cookie("raota_refresh_token", refreshTokenValue)
        .when()
                .post("/auth/refresh")
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", is("탈퇴 처리된 계정입니다. 탈퇴일로부터 30일 후 재가입할 수 있습니다."));
    }
}
