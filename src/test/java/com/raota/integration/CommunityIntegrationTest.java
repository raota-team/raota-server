package com.raota.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.presentation.api.community.dto.CommunityCommentCreateRequest;
import com.raota.presentation.api.community.dto.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.JpaCommentRepository;
import com.raota.domain.community.repository.command.JpaPostRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.infrastructure.auth.JwtTokenProvider;
import com.raota.helper.BaseIntegrationTest;
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
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String accessToken;
    private MemberProfile savedMember;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jpaCommentRepository.deleteAll();
        jpaPostRepository.deleteAll();
        ramenShopRepository.deleteAll();
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
                .contentType(ContentType.JSON)
                .body(request)
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
    @DisplayName("맛집후기 게시글 목록을 라멘집 ID로 필터링할 수 있다.")
    void get_review_posts_by_ramen_shop_id() {
        RamenShop targetShop = ramenShopRepository.save(sampleShop("멘야 하쿠"));
        RamenShop otherShop = ramenShopRepository.save(sampleShop("라멘 소라"));
        saveSamplePost("하쿠 후기", "내용 1", targetShop.getId());
        saveSamplePost("소라 후기", "내용 2", otherShop.getId());

        given()
                .param("category", "REVIEW")
                .param("ramenShopId", targetShop.getId())
                .param("page", 0)
                .param("size", 10)
        .when()
                .get("/community/posts")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(1))
                .body("data.items[0].ramenShopId", is(targetShop.getId().intValue()))
                .body("data.items[0].title", is("하쿠 후기"))
                .body("data.items[0].storeName", is("멘야 하쿠"));
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
        return saveSamplePost(title, content, null);
    }

    private Post saveSamplePost(String title, String content, Long ramenShopId) {
        return jpaPostRepository.save(Post.of(
                null, PostCategory.REVIEW, title, content, "PLAIN", null, savedMember.getId(), ramenShopId, LocalDateTime.now()
        ));
    }

    private RamenShop sampleShop(String name) {
        return RamenShop.builder()
                .name(name)
                .address(Address.of("서울", "마포구", "월드컵로", "1층"))
                .description("라멘집 설명")
                .imageUrl("https://example.com/shop.jpg")
                .build();
    }
}