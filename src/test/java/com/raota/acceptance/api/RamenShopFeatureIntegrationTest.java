package com.raota.acceptance.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.BusinessHours;
import com.raota.domain.ramenShop.model.EventMenus;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.NormalMenus;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.MenuVoteRepository;
import com.raota.domain.ramenlog.repository.RamenLogRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.infrastructure.auth.JwtTokenProvider;
import com.raota.support.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class RamenShopFeatureIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MenuVoteRepository menuVoteRepository;

    @Autowired
    private RamenLogRepository ramenLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String accessToken;
    private MemberProfile savedMember;
    private RamenShop savedShop;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        ramenLogRepository.deleteAll();
        menuVoteRepository.deleteAll();
        ramenShopRepository.deleteAll();
        memberRepository.deleteAll();

        savedMember = memberRepository.save(MemberProfile.builder()
                .nickname("부가기능테스터")
                .build());
        accessToken = jwtTokenProvider.createAccessToken(savedMember.getId());

        RamenShop shop = RamenShop.builder()
                .name("투표용 라멘집")
                .address(Address.of("서울", "마포구", "도로명", "상세"))
                .businessHours(BusinessHours.of("연중무휴", LocalTime.of(11, 0), LocalTime.of(21, 0), null, null, "불가"))
                .normalMenus(NormalMenus.init())
                .eventMenus(EventMenus.init())
                .build();
        
        shop.addNormalMenu(NormalMenu.builder().name("맛있는 라멘").price(10000).build());
        savedShop = ramenShopRepository.save(shop);
        ramenShopRepository.flush();
    }

    @Test
    @DisplayName("로그인한 사용자는 메뉴에 투표할 수 있으며, 투표 현황이 갱신된다.")
    void vote_menu_success() {
        Long menuId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_normal_menu WHERE ramen_shop_id = ? LIMIT 1", 
                Long.class, savedShop.getId());

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .post("/ramen-shops/{shopId}/votes/menus/{menuId}", savedShop.getId(), menuId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.total_votes", is(equalTo(1)));
    }

    @Test
    @DisplayName("이미 투표한 사용자가 같은 메뉴를 다시 투표하면 투표가 취소된다.")
    void vote_menu_toggle_success() {
        Long menuId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_normal_menu WHERE ramen_shop_id = ? LIMIT 1", 
                Long.class, savedShop.getId());

        // 첫 번째 투표
        given()
                .header("Authorization", "Bearer " + accessToken)
                .post("/ramen-shops/{shopId}/votes/menus/{menuId}", savedShop.getId(), menuId)
                .then().statusCode(HttpStatus.OK.value());

        // 두 번째 투표 (취소)
        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .post("/ramen-shops/{shopId}/votes/menus/{menuId}", savedShop.getId(), menuId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.total_votes", is(equalTo(0)));
    }

    @Test
    @DisplayName("비회원은 메뉴에 투표할 수 없다.")
    void vote_menu_anonymous_requires_login() {
        Long menuId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_normal_menu WHERE ramen_shop_id = ? LIMIT 1",
                Long.class, savedShop.getId());

        given()
        .when()
                .post("/ramen-shops/{shopId}/votes/menus/{menuId}", savedShop.getId(), menuId)
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("로그인한 사용자가 북마크한 라멘집 상세 조회 시 북마크 상태가 true로 반환된다.")
    void get_shop_detail_returns_bookmark_status_for_authenticated_member() {
        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .post("/ramen-shops/{shopId}/bookmark", savedShop.getId())
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data", is(true));

        given()
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("/ramen-shops/{shopId}", savedShop.getId())
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.is_bookmarked", is(true));
    }

    @Test
    @DisplayName("라멘집 인증샷을 업로드하면 업로드된 파일 정보를 반환한다.")
    void upload_proof_picture_success() {
        Map<String, String> request = Map.of(
                "imageUrl", "https://example.com/test.jpg",
                "description", "맛있는 라멘 인증!",
                "menuName", "맛있는 라멘"
        );

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/ramen-shops/{shopId}/photos", savedShop.getId())
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.image_url", notNullValue());
    }
}
