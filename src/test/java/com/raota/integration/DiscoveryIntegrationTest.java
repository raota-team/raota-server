package com.raota.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.community.repository.command.JpaPostRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.BusinessHours;
import com.raota.domain.ramenShop.model.RamenProofPicture;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenProofPictureRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.helper.BaseIntegrationTest;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class DiscoveryIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RamenProofPictureRepository ramenProofPictureRepository;

    @Autowired
    private JpaPostRepository jpaPostRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        ramenProofPictureRepository.deleteAll();
        jpaPostRepository.deleteAll();
        ramenShopRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("Discovery 통계 API가 정상적인 데이터를 반환한다.")
    void get_discovery_stats() {
        ramenShopRepository.save(sampleShop("가게1", "서울", "강남구"));
        memberRepository.save(MemberProfile.builder().nickname("유저1").build());
        
        given()
        .when()
                .get("/api/v1/discovery/stats")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true))
                .body("data.totalShops", is(1))
                .body("data.totalUsers", is(4000)) // 고정값 확인
                .body("data.totalReviews", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("인기 검색어 API가 플레이스홀더 데이터를 반환한다.")
    void get_trending_tags() {
        given()
                .param("limit", 3)
        .when()
                .get("/api/v1/discovery/trending-tags")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true))
                .body("data.size()", is(3))
                .body("data[0].name", is("토리파이탄"))
                .body("data[0].trend", is("up"));
    }

    @Test
    @DisplayName("최근 사진 인증된 라멘집 API가 정상 동작한다.")
    void get_recent_verified_shops() {
        RamenShop shop = ramenShopRepository.save(sampleShop("인증가게", "서울", "마포구"));
        MemberProfile member = memberRepository.save(MemberProfile.builder().nickname("유저1").build());
        
        ramenProofPictureRepository.save(RamenProofPicture.builder()
                .ramenShop(shop)
                .memberProfile(member)
                .imageUrl("https://test.com/img.jpg")
                .description("맛있어요")
                .menuName("돈코츠")
                .uploadedAt(LocalDateTime.now())
                .build());

        given()
                .param("limit", 4)
        .when()
                .get("/api/v1/shops/recent-verified")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true))
                .body("data.size()", is(1))
                .body("data[0].name", is("인증가게"))
                .body("data[0].imageUrl", is("https://test.com/img.jpg"))
                .body("data[0].photoCount", is(1));
    }

    @Test
    @DisplayName("커뮤니티 꿀팁 API가 필터링된 게시글을 반환한다.")
    void get_community_tips() {
        MemberProfile member = memberRepository.save(MemberProfile.builder().nickname("고수").build());
        
        // 꿀팁 게시글
        jpaPostRepository.save(Post.of(
                null, PostCategory.TIP, "꿀팁 제목", "꿀팁 내용입니다. 면을 꼬들하게 드세요.", "PLAIN", null, member.getId(), null, LocalDateTime.now()
        ));
        
        // 자유게시판 게시글 (필터링되어야 함)
        jpaPostRepository.save(Post.of(
                null, PostCategory.FREE, "자유 제목", "자유 내용", "PLAIN", null, member.getId(), null, LocalDateTime.now()
        ));

        given()
                .param("category", "tip")
                .param("limit", 3)
        .when()
                .get("/api/v1/community/posts")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true))
                .body("data.size()", is(1))
                .body("data[0].title", is("꿀팁 제목"))
                .body("data[0].author.nickname", is("고수"));
    }

    private RamenShop sampleShop(String name, String city, String district) {
        return RamenShop.builder()
                .name(name)
                .address(Address.of(city, district, "도로명", "상세"))
                .businessHours(BusinessHours.of("일요일", LocalTime.of(11, 0), LocalTime.of(20, 0), null, null, "불가"))
                .tags(List.of("기본"))
                .description("설명")
                .build();
    }
}
