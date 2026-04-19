package com.raota.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import com.raota.domain.community.presentation.request.CommunityCommentCreateRequest;
import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.JpaCommentRepository;
import com.raota.domain.community.repository.command.JpaPostRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.global.auth.JwtTokenProvider;
import com.raota.global.common.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
        memberRepository.deleteAll();

        savedMember = memberRepository.save(MemberProfile.builder()
                .nickname("마이페이지테스터")
                .build());
        accessToken = jwtTokenProvider.createAccessToken(savedMember.getId());
    }

    @Test
    @DisplayName("커뮤니티 활동(글/댓글 작성) 시 마이페이지의 통계 정보가 실시간으로 업데이트된다.")
    void my_activity_stats_update_realtime() {
        CommunityPostCreateRequest postRequest = new CommunityPostCreateRequest(
                "REVIEW", null, "마이페이지 제목", null, "PLAIN", "내용"
        );
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.MULTIPART)
                .multiPart("request", postRequest, "application/json; charset=UTF-8")
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
}
