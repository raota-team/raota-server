package com.raota.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.raota.presentation.api.community.request.CommunityCommentCreateRequest;
import com.raota.presentation.api.community.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.JpaCommentRepository;
import com.raota.domain.community.repository.command.JpaPostRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenlog.model.RamenLog;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenlog.repository.RamenLogRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.infrastructure.auth.JwtTokenProvider;
import com.raota.helper.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class MemberIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JpaPostRepository jpaPostRepository;

    @Autowired
    private JpaCommentRepository jpaCommentRepository;

    @Autowired
    private RamenLogRepository ramenLogRepository;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String accessToken;
    private MemberProfile savedMember;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jpaCommentRepository.deleteAll();
        jpaPostRepository.deleteAll();
        ramenLogRepository.deleteAll();
        ramenShopRepository.deleteAll();
        memberRepository.deleteAll();

        savedMember = memberRepository.save(MemberProfile.builder()
                .nickname("마이페이지테스터")
                .build());
        accessToken = jwtTokenProvider.createAccessToken(savedMember.getId());
    }

    @Test
    @DisplayName("로그인한 사용자는 자신의 요약 정보(닉네임, 프로필 이미지)를 조회할 수 있다.")
    void get_my_summary_success() {
        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/summary")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true))
                .body("data.nickname", is("마이페이지테스터"))
                .body("data.profileImageUrl", is(savedMember.getImageUrl()));
    }

    @Test
    @DisplayName("회원 탈퇴 시 소프트 딜리트 처리되고 이후 인증 요청은 차단된다.")
    void withdraw_member_success() {
        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .delete("/users/me")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true))
                .body("message", is("회원 탈퇴가 완료되었습니다. 탈퇴일로부터 30일 후 재가입할 수 있습니다."));

        MemberProfile withdrawnMember = memberRepository.findById(savedMember.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(withdrawnMember.getDeletedAt()).isNotNull();

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/profile")
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", is("탈퇴 처리된 계정입니다. 탈퇴일로부터 30일 후 재가입할 수 있습니다."));
    }

    @Test
    @DisplayName("커뮤니티 활동(글/댓글 작성) 시 마이페이지의 통계 정보가 실시간으로 업데이트된다.")
    void my_activity_stats_update_realtime() {
        CommunityPostCreateRequest postRequest = new CommunityPostCreateRequest(
                "REVIEW", null, "마이페이지 제목", null, "PLAIN", "내용"
        );
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(postRequest)
                .post("/community/posts")
                .then().statusCode(HttpStatus.OK.value());

        Long realPostId = jdbcTemplate.queryForObject("SELECT id FROM tb_post WHERE title = '마이페이지 제목' LIMIT 1", Long.class);

        CommunityCommentCreateRequest commentRequest = new CommunityCommentCreateRequest("댓글달기", null);
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(commentRequest)
                .post("/community/posts/{postId}/comments", realPostId)
                .then().statusCode(HttpStatus.OK.value());

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/profile")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.nickname", is("마이페이지테스터"))
                .body("data.stats.post_count", is(1))
                .body("data.stats.comment_count", is(1));
    }

    @Test
    @DisplayName("내가 작성한 글과 댓글 리스트를 정상적으로 조회할 수 있다.")
    void get_my_posts_and_comments_list() {
        my_activity_stats_update_realtime();

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/posts")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(1))
                .body("data.items[0].title", is("마이페이지 제목"));

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/comments")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(1))
                .body("data.items[0].content", is("댓글달기"));
    }

    @Test
    @DisplayName("삭제된 게시글과 그 게시글의 댓글은 마이페이지 목록과 통계에서 제외된다.")
    void deleted_post_and_its_comments_are_excluded_from_my_page() {
        CommunityPostCreateRequest postRequest = new CommunityPostCreateRequest(
                "REVIEW", null, "삭제될 게시글", null, "PLAIN", "내용"
        );
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(postRequest)
                .post("/community/posts")
                .then().statusCode(HttpStatus.OK.value());

        Long postId = jdbcTemplate.queryForObject("SELECT id FROM tb_post WHERE title = '삭제될 게시글' LIMIT 1", Long.class);

        CommunityCommentCreateRequest commentRequest = new CommunityCommentCreateRequest("숨겨질 댓글", null);
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(commentRequest)
                .post("/community/posts/{postId}/comments", postId)
                .then().statusCode(HttpStatus.OK.value());

        given()
                .header("Authorization", "Bearer " + accessToken)
                .delete("/community/posts/{postId}", postId)
                .then().statusCode(HttpStatus.OK.value());

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/profile")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.stats.post_count", is(0))
                .body("data.stats.comment_count", is(0));

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/posts")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(0));

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/comments")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(0));
    }

    @Test
    @DisplayName("사용자 ID로 공개 프로필/활동 목록을 조회할 수 있다.")
    void get_user_public_profile_and_activity_lists_by_user_id() {
        CommunityPostCreateRequest postRequest = new CommunityPostCreateRequest(
                "REVIEW", null, "공개 조회 게시글", null, "PLAIN", "내용"
        );
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(postRequest)
                .post("/community/posts")
                .then().statusCode(HttpStatus.OK.value());

        Long postId = jdbcTemplate.queryForObject("SELECT id FROM tb_post WHERE title = '공개 조회 게시글' LIMIT 1", Long.class);

        CommunityCommentCreateRequest commentRequest = new CommunityCommentCreateRequest("공개 조회 댓글", null);
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(commentRequest)
                .post("/community/posts/{postId}/comments", postId)
                .then().statusCode(HttpStatus.OK.value());

        RamenShop ramenShop = ramenShopRepository.save(RamenShop.builder()
                .name("공개 조회 라멘집")
                .address(Address.of("서울", "마포구", "테스트로 1", null))
                .imageUrl("https://images.example.com/shop.jpg")
                .build());
        ramenLogRepository.save(RamenLog.builder()
                .ramenShop(ramenShop)
                .author(savedMember)
                .imageUrl("https://images.example.com/proof.jpg")
                .note("공개 조회 인증샷")
                .menuName("시오라멘")
                .build());

        Long userId = savedMember.getId();

        given()
        .when()
                .get("/users/{userId}/profile", userId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.nickname", is("마이페이지테스터"))
                .body("data.stats.post_count", is(1))
                .body("data.stats.comment_count", is(1))
                .body("data.stats.total_photo_count", is(1))
                .body("data.stats.visited_restaurant_count", is(1));

        given()
        .when()
                .get("/users/{userId}/posts", userId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(1))
                .body("data.items[0].title", is("공개 조회 게시글"));

        given()
        .when()
                .get("/users/{userId}/comments", userId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(1))
                .body("data.items[0].content", is("공개 조회 댓글"));

        given()
        .when()
                .get("/users/{userId}/photos", userId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(1))
                .body("data.items[0].image_url", is("https://images.example.com/proof.jpg"));

        given()
        .when()
                .get("/users/{userId}/visits", userId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(1))
                .body("data.items[0].restaurant_name", is("공개 조회 라멘집"));
    }

    @Test
    @DisplayName("활동 카테고리를 비공개로 설정하면 타 사용자 조회와 통계를 차단한다.")
    void private_activity_categories_are_hidden_from_other_users() {
        Long userId = savedMember.getId();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "logs", false,
                        "visits", false,
                        "posts", false,
                        "comments", false
                ))
        .when()
                .patch("/users/me/privacy-settings")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.logs", is(false))
                .body("data.visits", is(false))
                .body("data.posts", is(false))
                .body("data.comments", is(false));

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/users/me/privacy-settings")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.logs", is(false));

        given()
        .when()
                .get("/users/{userId}/profile", userId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.activity_visibility.logs", is(false))
                .body("data.stats.total_log_count", nullValue())
                .body("data.stats.visited_restaurant_count", nullValue())
                .body("data.stats.post_count", nullValue())
                .body("data.stats.comment_count", nullValue());

        given().get("/users/{userId}/photos", userId).then().statusCode(HttpStatus.FORBIDDEN.value());
        given().get("/users/{userId}/visits", userId).then().statusCode(HttpStatus.FORBIDDEN.value());
        given().get("/users/{userId}/posts", userId).then().statusCode(HttpStatus.FORBIDDEN.value());
        given().get("/users/{userId}/comments", userId).then().statusCode(HttpStatus.FORBIDDEN.value());
        given().get("/users/{userId}/ramen-logs", userId).then().statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .header("Authorization", "Bearer " + accessToken)
                .get("/users/{userId}/ramen-logs", userId)
        .then()
                .statusCode(HttpStatus.OK.value());
    }
}
