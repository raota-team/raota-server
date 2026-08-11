package com.raota.acceptance.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import com.raota.ramenshop.domain.model.Address;
import com.raota.ramenshop.domain.model.BusinessHours;
import com.raota.ramenshop.domain.model.EventMenus;
import com.raota.ramenshop.domain.model.NormalMenus;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.support.BaseIntegrationTest;
import io.restassured.RestAssured;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class RamenShopIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        ramenShopRepository.deleteAll();
    }

    @Test
    @DisplayName("city 필터링 시 해당 지역의 라멘집 리스트와 한줄평(설명), 태그가 정상 반환된다.")
    void get_ramen_shop_list_with_filters() {
        RamenShop shop = sampleShop("멘야 하쿠", "서울", "성동구");
        shop.updateBasicInfo("멘야 하쿠", "본점", "naver-123", shop.getAddress(), shop.getBusinessHours(),
                List.of("토리파이탄", "혼밥"), null, null, "진한 국물 맛집", "https://img.com");
        ramenShopRepository.save(shop);
        ramenShopRepository.save(sampleShop("이리에 라멘", "서울", "마포구"));
        ramenShopRepository.save(sampleShop("멘야 카네토라", "부산", "해운대구"));

        given()
                .param("city", "서울")
        .when()
                .get("/ramen-shops")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(2))
                .body("data.items.name", hasItems("멘야 하쿠", "이리에 라멘"))
                .body("data.items.find { it.name == '멘야 하쿠' }.tagLine", is("진한 국물 맛집"))
                .body("data.items.find { it.name == '멘야 하쿠' }.tags", hasItems("토리파이탄", "혼밥"))
                .body("data.items.find { it.name == '멘야 하쿠' }.viewCount", is(0))
                .body("data.items.region", hasItems("서울 성동구", "서울 마포구"));
    }

    @Test
    @DisplayName("city와 district를 함께 넘기면 해당 행정구역의 라멘집만 반환된다.")
    void get_ramen_shop_list_with_city_and_district_filters() {
        ramenShopRepository.save(sampleShop("멘야 하쿠", "서울", "성동구"));
        ramenShopRepository.save(sampleShop("이리에 라멘", "서울", "마포구"));
        ramenShopRepository.save(sampleShop("멘야 카네토라", "부산", "해운대구"));

        given()
                .param("city", "서울")
                .param("district", "성동구")
        .when()
                .get("/ramen-shops")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(1))
                .body("data.items[0].name", is("멘야 하쿠"))
                .body("data.items[0].region", is("서울 성동구"));
    }

    @Test
    @DisplayName("라멘집 상세 조회 시 지점명과 네이버 ID 등 확장 필드가 포함된다.")
    void get_ramen_shop_detail_with_extra_fields() {
        RamenShop shop = sampleShop("멘야 하쿠", "서울", "성동구");
        shop.updateBasicInfo("멘야 하쿠", "성수점", "naver-999", shop.getAddress(), shop.getBusinessHours(),
                List.of("태그1", "태그2"), "https://insta", "https://catch", "가게설명", "https://img");
        RamenShop savedShop = ramenShopRepository.save(shop);

        given()
        .when()
                .post("/ramen-shops/{shopId}/views", savedShop.getId())
        .then()
                .statusCode(HttpStatus.OK.value());

        given()
        .when()
                .get("/ramen-shops/{shopId}", savedShop.getId())
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.name", is("멘야 하쿠"))
                .body("data.branch_name", is("성수점"))
                .body("data.naver_map_id", is("naver-999"))
                .body("data.stats.view_count", is(1))
                .body("data.tags", hasItems("태그1", "태그2"));
    }

    @Test
    @DisplayName("숨김 처리된 라멘집은 일반 목록에 노출되지 않는다.")
    void hidden_shop_is_excluded_from_public_list() {
        RamenShop hiddenShop = RamenShop.builder()
                .name("숨김 라멘")
                .address(Address.of("서울", "성동구", "도로명", "상세"))
                .businessHours(BusinessHours.of("일요일", LocalTime.of(11, 0), LocalTime.of(20, 0), null, null, "불가"))
                .tags(List.of("기본"))
                .description("설명")
                .published(false)
                .normalMenus(NormalMenus.init())
                .eventMenus(EventMenus.init())
                .build();
        ramenShopRepository.save(hiddenShop);

        given()
                .param("city", "서울")
        .when()
                .get("/ramen-shops")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", is(0));
    }

    private RamenShop sampleShop(String name, String city, String district) {
        return RamenShop.builder()
                .name(name)
                .address(Address.of(city, district, "도로명", "상세"))
                .businessHours(BusinessHours.of("일요일", LocalTime.of(11, 0), LocalTime.of(20, 0), null, null, "불가"))
                .tags(List.of("기본"))
                .description("설명")
                .normalMenus(NormalMenus.init())
                .eventMenus(EventMenus.init())
                .build();
    }
}
