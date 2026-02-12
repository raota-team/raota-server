package com.raota.Integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.raota.domain.member.model.MemberActivityStats;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.BusinessHours;
import com.raota.domain.ramenShop.model.EventMenus;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.NormalMenus;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.model.ShopStats;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.vote.model.MenuVote;
import com.raota.domain.vote.repository.MenuVoteRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public class VoteIntegrationTest {

    @LocalServerPort
    int port;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired private RamenShopRepository ramenShopRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MenuVoteRepository voteRepository;

    private Long targetShopId;
    private Long memberId;
    private NormalMenu signatureMenu;
    private NormalMenu limitedMenu;
    private NormalMenu thirdMenu;

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

    @Test
    @DisplayName("통합 : 메뉴 투표 현황 조회 시 득표수와 퍼센트가 정확히 계산되어 반환된다")
    void retrieveMenuVoteStatus() {
        RamenShop shop = ramenShopRepository.findById(targetShopId).orElseThrow();
        MemberProfile voter1 = createAndSaveMember("유권자1");
        MemberProfile voter2 = createAndSaveMember("유권자2");

        saveVote(signatureMenu, voter1);
        saveVote(signatureMenu, voter2);
        saveVote(signatureMenu, memberRepository.findById(memberId).get());

        saveVote(limitedMenu, voter1);
        saveVote(limitedMenu, voter2);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .pathParam("shopId", targetShopId)

                .when()
                .get("/votes/{shopId}",targetShopId)

                .then().log().all()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))

                // [Then] 3. 전체 투표 수 검증
                .body("data.total_votes", equalTo(5))

                // [Then] 4. 메뉴1 (돈코츠) 검증: 3표, 60%
                .body("data.vote_results.find { it.menu_id == " + signatureMenu.getId() + " }.menu_name", equalTo("돈코츠 라멘"))
                .body("data.vote_results.find { it.menu_id == " + signatureMenu.getId() + " }.vote_count", equalTo(3))
                .body("data.vote_results.find { it.menu_id == " + signatureMenu.getId() + " }.percentage", is(60.0f))

                // [Then] 5. 메뉴2 (쇼유) 검증: 2표, 40%
                .body("data.vote_results.find { it.menu_id == " + limitedMenu.getId() + " }.vote_count", equalTo(2))
                .body("data.vote_results.find { it.menu_id == " + limitedMenu.getId() + " }.percentage", is(40.0f))

                // [Then] 6. 메뉴3 (시오) 검증: 0표, 0%
                .body("data.vote_results.find { it.menu_id == " + thirdMenu.getId() + " }.vote_count", equalTo(0))
                .body("data.vote_results.find { it.menu_id == " + thirdMenu.getId() + " }.percentage", is(0.0f));
    }

    @Test
    @DisplayName("통합 : 투표 API 호출 후 메뉴 투표 현황의 득표수와 퍼센트가 갱신되어 반환된다")
    void voteMenuAndRetrieveUpdatedStatus() {
        MemberProfile voter1 = createAndSaveMember("유권자1");
        MemberProfile voter2 = createAndSaveMember("유권자2");

        // 시그니처 메뉴 3표
        saveVote(signatureMenu, voter1);
        saveVote(signatureMenu, voter2);
        saveVote(signatureMenu, memberRepository.findById(memberId).orElseThrow());

        // 쇼유 메뉴 2표
        saveVote(limitedMenu, voter1);
        saveVote(limitedMenu, voter2);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .pathParam("shopId", targetShopId)
                .pathParam("menuId", signatureMenu.getId())
                .header("X-User-Id", memberId)
                .when()
                .post("/votes/{shopId}/menus/{menuId}", targetShopId, signatureMenu.getId())

                .then().log().all()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))

                .body("data.total_votes", equalTo(6))

                .body("data.vote_results.find { it.menu_id == " + signatureMenu.getId() + " }.vote_count",
                        equalTo(4))
                .body("data.vote_results.find { it.menu_id == " + signatureMenu.getId() + " }.percentage",
                        is(equalTo(66.67f)))

                .body("data.vote_results.find { it.menu_id == " + limitedMenu.getId() + " }.vote_count",
                        equalTo(2))
                .body("data.vote_results.find { it.menu_id == " + limitedMenu.getId() + " }.percentage",
                        is(equalTo(33.33f)))

                .body("data.vote_results.find { it.menu_id == " + thirdMenu.getId() + " }.vote_count",
                        equalTo(0))
                .body("data.vote_results.find { it.menu_id == " + thirdMenu.getId() + " }.percentage",
                        is(equalTo(0f)));
    }

    private void clearDatabase() {
        voteRepository.deleteAllInBatch();
        ramenShopRepository.deleteAll();
        memberRepository.deleteAll();
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

        signatureMenu = NormalMenu.builder()
                .name("돈코츠 라멘")
                .price(10000)
                .isSignature(true)
                .imageUrl("https://default.image.url/menu/signature")
                .build();

        limitedMenu = NormalMenu.builder()
                .name("쇼유 라멘")
                .price(12000)
                .isSignature(false)
                .imageUrl("https://default.image.url/menu/shoyu")
                .build();

       thirdMenu = NormalMenu.builder()
                .name("시오 라멘")
                .price(10000)
                .isSignature(false)
                .imageUrl("https://default.image.url/menu/shoyu")
                .build();

        shop.addNormalMenu(signatureMenu);
        shop.addNormalMenu(limitedMenu);
        shop.addNormalMenu(thirdMenu);

        return shop;
    }

    private MemberProfile createAndSaveMember(String nickname) {
        MemberProfile member = MemberProfile.builder()
                .nickname(nickname)
                .stats(MemberActivityStats.init())
                .imageUrl("https://test.image/" + nickname)
                .build();
        return memberRepository.save(member);
    }

    private void saveVote(NormalMenu menu, MemberProfile voter) {
        MenuVote vote = MenuVote.builder()
                .normalMenu(menu)
                .memberProfile(voter)
                .build();
        voteRepository.save(vote);
    }
}
