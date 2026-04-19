package com.raota.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class CommunityIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JpaPostRepository jpaPostRepository;

    @Autowired
    private JpaCommentRepository jpaCommentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String accessToken;
    private MemberProfile savedMember;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jpaCommentRepository.deleteAll();
        jpaPostRepository.deleteAll();
        memberRepository.deleteAll();

        savedMember = memberRepository.save(MemberProfile.builder()
                .nickname("커뮤니티테스터")
                .build());
        accessToken = jwtTokenProvider.createAccessToken(savedMember.getId());
    }

    @Test
    @DisplayName("로그인한 사용자는 리뷰 게시판에 글을 작성할 수 있다.")
    void create_post_success() {
        CommunityPostCreateRequest request = new CommunityPostCreateRequest(
                "REVIEW", null, "테스트 제목", null, "PLAIN", "테스트 내용입니다."
        );

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.MULTIPART)
                .multiPart("request", request, "application/json; charset=UTF-8")
        .when()
                .post("/community/posts")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.title", is("테스트 제목"))
                .body("data.authorName", is("커뮤니티테스터"));
    }

    @Test
    @DisplayName("게시글 목록을 카테고리별로 페이징 조회할 수 있다.")
    void get_posts_with_paging() {
        saveSamplePost("제목 1", "내용 1");
        saveSamplePost("제목 2", "내용 2");

        given()
                .param("category", "REVIEW")
                .param("page", 0)
                .param("size", 10)
        .when()
                .get("/community/posts")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(2));
    }

    @Test
    @DisplayName("로그인한 사용자는 게시글에 댓글을 작성할 수 있다.")
    void create_comment_success() {
        Post post = saveSamplePost("댓글용 글", "내용");
        CommunityCommentCreateRequest request = new CommunityCommentCreateRequest("댓글 내용입니다.", null);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/community/posts/{postId}/comments", post.getId())
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.content", is("댓글 내용입니다."))
                .body("data.authorNickname", is("커뮤니티테스터"));
    }

    private Post saveSamplePost(String title, String content) {
        return jpaPostRepository.save(Post.of(
                null, PostCategory.REVIEW, title, content, "PLAIN", null, savedMember.getId(), null, LocalDateTime.now()
        ));
    }
}
