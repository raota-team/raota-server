package com.raota.Integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.raota.domain.member.model.MemberActivityStats;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.BusinessHours;
import com.raota.domain.ramenShop.model.EventMenu;
import com.raota.domain.ramenShop.model.EventMenus;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.NormalMenus;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.model.ShopStats;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public class RamenShopIntegrationTest {

    @LocalServerPort
    int port;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired private RamenShopRepository ramenShopRepository;
    @Autowired private MemberRepository memberRepository;

    private Long targetShopId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        clearDatabase();
        MemberProfile member = MemberProfile.builder()
                .nickname("바키")
                .stats(MemberActivityStats.init())
                .imageUrl("https://original-image.com")
                .build();

        MemberProfile savedMember = memberRepository.save(member);
        memberId = savedMember.getId();

        RamenShop shop = ramenShopRepository.save(createRamenShop("부탄츄",1,2));
        targetShopId = shop.getId();
    }

    private void clearDatabase() {
        ramenShopRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("통합: 라멘집 기본 상세정보를 반환한다")
    void get_shop_detail_integration() {
        RestAssured.given().log().all()
                .accept(ContentType.JSON)

                .when()
                .get("/ramen-shops/{shopId}", targetShopId)

                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("SUCCESS"))

                .body("data.id", equalTo(targetShopId.intValue()))
                .body("data.name", equalTo("부탄츄"))
                .body("data.address", equalTo("서울 마포구 동교로 100 B1"))

                .body("data.stats.visit_count", equalTo(1))
                .body("data.stats.bookmark_count", equalTo(2))

                .body("data.tags", hasSize(2))
                .body("data.tags", hasItems("돈코츠", "자가제면"))

                .body("data.normal_menus", hasSize(2))
                .body("data.normal_menus[0].name", equalTo("돈코츠 라멘"))

                .body("data.event_menus", hasSize(1))
                .body("data.event_menus[0].badge_text", equalTo("봄 한정"));
    }

    @Test
    @DisplayName("통합: 라멘 가게 리스트 조회 - 페이징과 내용이 DB와 일치해야 한다")
    void get_ramen_shop_list_integration() {
        RamenShop shop2 = RamenShop.builder()
                .name("라멘스키 강남점")
                .address(new Address("서울","강남구","논현동","123"))
                .stats(ShopStats.init())
                .build();
        ramenShopRepository.save(shop2);

        RestAssured.given().log().all()
                .param("page", "0")
                .param("size", "2")
                .accept(ContentType.JSON)
                .when()
                .get("/ramen-shops")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("SUCCESS"))

                .body("data.content", hasSize(2))

                .body("data.totalElements", equalTo(2))
                .body("data.size", equalTo(2))
                .body("data.number", equalTo(0))
                .body("data.totalPages", equalTo(1));
    }


    private RamenShop createRamenShop(String name, int initialVisitCount, int initialBookmarkCount) {
        Address address = Address.of("서울", "마포구", "동교로 100", "B1");
        BusinessHours hours = BusinessHours.of("수요일", LocalTime.of(11, 30), LocalTime.of(21, 0), LocalTime.of(15, 0), LocalTime.of(17, 0));
        ShopStats stats = new ShopStats(initialVisitCount, initialBookmarkCount);
        List<String> tags = List.of("돈코츠", "자가제면");

        RamenShop shop = RamenShop.builder()
                .name(name)
                .address(address)
                .businessHours(hours)
                .stats(stats)
                .tags(tags)
                .instagramUrl("https://instagram.com/" + name)
                .imageUrl("https://default.image.url/shop/" + name)
                .normalMenus(NormalMenus.init())
                .eventMenus(EventMenus.init())
                .build();

        NormalMenu signatureMenu = NormalMenu.builder()
                .name("돈코츠 라멘")
                .price(10000)
                .isSignature(true)
                .imageUrl("https://default.image.url/menu/signature")
                .build();

        NormalMenu limitedMenu = NormalMenu.builder()
                .name("쇼유 라멘")
                .price(12000)
                .isSignature(false)
                .imageUrl("https://default.image.url/menu/shoyu")
                .build();

        shop.addNormalMenu(signatureMenu);
        shop.addNormalMenu(limitedMenu);

        EventMenu seasonalEvent = EventMenu.builder()
                .name("벚꽃 라멘")
                .price(14000)
                .badgeText("봄 한정")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        shop.addEventMenu(seasonalEvent);

        return shop;
    }
}
