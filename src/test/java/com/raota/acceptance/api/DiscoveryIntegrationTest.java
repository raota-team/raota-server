package com.raota.acceptance.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import com.raota.community.domain.model.Post;
import com.raota.community.domain.model.PostCategory;
import com.raota.community.infrastructure.persistence.command.JpaPostRepository;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.ramenshop.domain.model.Address;
import com.raota.ramenshop.domain.model.BusinessHours;
import com.raota.ramenlog.domain.model.RamenLog;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenlog.domain.repository.RamenLogRepository;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.support.BaseIntegrationTest;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

class DiscoveryIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RamenLogRepository ramenLogRepository;

    @Autowired
    private JpaPostRepository jpaPostRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        Set<String> rankingKeys = redisTemplate.keys("ramen-shop:view:*");
        if (rankingKeys != null && !rankingKeys.isEmpty()) {
            redisTemplate.delete(rankingKeys);
        }
        ramenLogRepository.deleteAll();
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
    @DisplayName("오늘 많이 본 라멘집 API가 상세 조회수 상위 라멘집을 반환한다.")
    void get_today_popular_shops() {
        RamenShop first = ramenShopRepository.save(sampleShop("가장 많이 본 집", "서울", "마포구"));
        RamenShop second = ramenShopRepository.save(sampleShop("두번째 집", "서울", "성동구"));

        given().when().post("/ramen-shops/{shopId}/views", first.getId()).then().statusCode(HttpStatus.OK.value());
        given().when().post("/ramen-shops/{shopId}/views", first.getId()).then().statusCode(HttpStatus.OK.value());
        given().when().post("/ramen-shops/{shopId}/views", second.getId()).then().statusCode(HttpStatus.OK.value());

        given()
                .param("limit", 5)
        .when()
                .get("/api/v1/discovery/popular-shops/today")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("success", is(true))
                .body("data.size()", is(2))
                .body("data[0].ramenShopId", is(first.getId().intValue()))
                .body("data[0].name", is("가장 많이 본 집"))
                .body("data[1].ramenShopId", is(second.getId().intValue()));
    }

    @DisplayName("최근 사진 인증된 라멘집 API가 정상 동작한다.")
    void get_recent_verified_shops() {
        RamenShop shop = ramenShopRepository.save(sampleShop("인증가게", "서울", "마포구"));
        MemberProfile member = memberRepository.save(MemberProfile.builder().nickname("유저1").build());
        
        ramenLogRepository.save(RamenLog.builder()
                .ramenShop(shop)
                .author(member)
                .imageUrl("https://test.com/img.jpg")
                .note("맛있어요")
                .menuName("돈코츠")
                .createdAt(LocalDateTime.now())
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
                null, PostCategory.TIP, "꿀팁 제목", "꿀팁 내용입니다. 면을 꼬들하게 드세요.", "PLAIN", null, member.getId(), null, 0, LocalDateTime.now()
        ));
        
        // 자유게시판 게시글 (필터링되어야 함)
        jpaPostRepository.save(Post.of(
                null, PostCategory.FREE, "자유 제목", "자유 내용", "PLAIN", null, member.getId(), null, 0, LocalDateTime.now()
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
