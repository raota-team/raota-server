package com.raota.global.auth;

import com.raota.domain.auth.model.RefreshToken;
import com.raota.domain.auth.repository.RefreshTokenRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.global.common.ApiResponse;
import io.restassured.RestAssured;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
    @DisplayName("유효한 Refresh Token으로 Access Token을 갱신한다.")
    void refresh_token_success() {
        // given
        MemberProfile member = memberRepository.save(MemberProfile.builder()
                .nickname("testuser")
                .build());

        String refreshTokenValue = "valid-refresh-token";
        refreshTokenRepository.save(RefreshToken.builder()
                .memberId(member.getId())
                .token(refreshTokenValue)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build());

        // when
        RestAssured.given()
                .cookie("raota_refresh_token", refreshTokenValue)
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}
