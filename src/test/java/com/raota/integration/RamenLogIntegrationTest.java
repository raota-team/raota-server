package com.raota.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.ramenlog.repository.RamenLogLikeRepository;
import com.raota.domain.ramenlog.repository.RamenLogRepository;
import com.raota.helper.BaseIntegrationTest;
import com.raota.infrastructure.auth.JwtTokenProvider;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class RamenLogIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired private RamenLogLikeRepository ramenLogLikeRepository;
    @Autowired private RamenLogRepository ramenLogRepository;
    @Autowired private RamenShopRepository ramenShopRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private MemberProfile member;
    private RamenShop shop;
    private String accessToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        ramenLogLikeRepository.deleteAll();
        ramenLogRepository.deleteAll();
        ramenShopRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(MemberProfile.builder()
                .nickname("라멘로그테스터")
                .build());
        shop = ramenShopRepository.save(RamenShop.builder()
                .name("멘야 로그")
                .address(Address.of("서울", "마포구", "월드컵로", "1층"))
                .build());
        accessToken = jwtTokenProvider.createAccessToken(member.getId());
    }

    @Test
    void ramenLogCrudVisibilityAndLikeFlow() {
        Long logId = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(payload(true))
        .when()
                .post("/ramen-logs")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.menuName", equalTo("특제 돈코츠"))
                .body("data.public", equalTo(true))
                .body("data.mine", equalTo(true))
                .extract()
                .jsonPath()
                .getLong("data.id");

        given()
        .when()
                .get("/ramen-logs?size=8&sort=LATEST")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(1))
                .body("data.items[0].liked", equalTo(false))
                .body("data.items[0].mine", equalTo(false));

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .post("/ramen-logs/{logId}/likes", logId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.liked", equalTo(true))
                .body("data.likeCount", equalTo(1));

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(payload(false))
        .when()
                .patch("/ramen-logs/{logId}", logId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.public", equalTo(false));

        given()
        .when()
                .get("/ramen-logs?size=8")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(0));

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/ramen-logs?size=8")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(1))
                .body("data.items[0].public", equalTo(false))
                .body("data.items[0].liked", equalTo(true));
    }

    private Map<String, Object> payload(boolean isPublic) {
        return Map.of(
                "shopId", shop.getId(),
                "menuName", "특제 돈코츠",
                "ramenType", "돈코츠",
                "imageUrl", "proof/ramen-log.webp",
                "note", "진하지만 끝맛이 깔끔했다.",
                "tasteNotes", Map.of(
                        "broth", List.of("진해요"),
                        "noodle", List.of("단단해요"),
                        "seasoning", List.of("딱 좋아요"),
                        "topping", List.of("차슈 좋아요")
                ),
                "revisit", "DEFINITELY",
                "public", isPublic
        );
    }
}
