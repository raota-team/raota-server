package com.raota.presentation.api.ramenShop;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.BusinessHours;
import com.raota.domain.ramenShop.model.EventMenu;
import com.raota.domain.ramenShop.model.EventMenus;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.NormalMenus;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenlog.model.RamenLog;
import com.raota.domain.ramenlog.repository.RamenLogRepository;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;



import com.raota.helper.BaseIntegrationTest;

@Transactional
class RamenShopInfoControllerTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private RamenLogRepository ramenLogRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        ramenLogRepository.deleteAll();
        ramenShopRepository.deleteAll();
    }

    @Test
    void searchByCityReturnsMatchingItemsOnly() throws Exception {
        RamenShop shop = sampleShop("멘야 하쿠", "서울", "성동구");
        shop.updateBasicInfo("멘야 하쿠", "본점", "12345", shop.getAddress(), shop.getBusinessHours(), 
                List.of("토리파이탄", "혼밥"), null, null, "진한 국물 맛집", null);
        ramenShopRepository.save(shop);
        ramenShopRepository.save(sampleShop("이리에 라멘", "서울", "마포구"));
        ramenShopRepository.save(sampleShop("멘야 카네토라", "부산", "해운대구"));

        mockMvc.perform(get("/ramen-shops")
                        .param("city", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[*].name").value(hasItems("멘야 하쿠", "이리에 라멘")))
                .andExpect(jsonPath("$.data.items[?(@.name=='멘야 하쿠')].tagLine").value(hasItems("진한 국물 맛집"))) // 설명이 한줄평으로 나오는지 확인
                .andExpect(jsonPath("$.data.items[?(@.name=='멘야 하쿠')].tags[*]").value(hasItems("토리파이탄", "혼밥"))) // 태그 확인
                .andExpect(jsonPath("$.data.items[*].region").value(hasItems("서울 성동구", "서울 마포구")));
    }

    @Test
    void searchByCityAndDistrictReturnsMatchingItemsOnly() throws Exception {
        ramenShopRepository.save(sampleShop("멘야 하쿠", "서울", "성동구"));
        ramenShopRepository.save(sampleShop("이리에 라멘", "서울", "마포구"));
        ramenShopRepository.save(sampleShop("멘야 카네토라", "부산", "해운대구"));

        mockMvc.perform(get("/ramen-shops")
                        .param("city", "서울")
                        .param("district", "성동구"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("멘야 하쿠"))
                .andExpect(jsonPath("$.data.items[0].region").value("서울 성동구"));
    }

    @Test
    void getShopDetailInfoReturnsExtraFields() throws Exception {
        RamenShop shop = sampleShop("멘야 하쿠", "서울", "성동구");
        shop.updateBasicInfo("멘야 하쿠", "성수점", "naver-123", shop.getAddress(), shop.getBusinessHours(),
                List.of("태그1", "태그2"), "https://insta", "https://catch", "가게설명", "https://img");
        RamenShop savedShop = ramenShopRepository.save(shop);

        mockMvc.perform(get("/ramen-shops/{shopId}", savedShop.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("멘야 하쿠"))
                .andExpect(jsonPath("$.data.branch_name").value("성수점")) // 지점명 확인
                .andExpect(jsonPath("$.data.naver_map_id").value("naver-123")) // 네이버 ID 확인
                .andExpect(jsonPath("$.data.tags").value(hasItems("태그1", "태그2"))); // 전체 태그 확인
    }

    @Test
    void getShopDetailInfoDoesNotReturnMenuImages() throws Exception {
        RamenShop shop = sampleShop("멘야 하쿠", "서울", "성동구");
        shop.addEventMenu(EventMenu.builder()
                .name("한정 츠케멘")
                .description("기간 한정 메뉴")
                .price(13000)
                .badgeText("LIMITED")
                .imageUrl("https://example.com/event-menu.jpg")
                .build());
        RamenShop savedShop = ramenShopRepository.save(shop);

        mockMvc.perform(get("/ramen-shops/{shopId}", savedShop.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.normal_menus[0].name").value("기본 라멘"))
                .andExpect(jsonPath("$.data.normal_menus[0].image_url").doesNotExist())
                .andExpect(jsonPath("$.data.event_menus[0].name").value("한정 츠케멘"))
                .andExpect(jsonPath("$.data.event_menus[0].image_url").doesNotExist());
    }

    @Test
    void searchByKeywordReturnsMatchingItemsOnly() throws Exception {
        ramenShopRepository.save(sampleShop("멘야 하쿠", "서울", "성동구"));
        ramenShopRepository.save(sampleShop("이리에 라멘", "서울", "마포구"));

        mockMvc.perform(get("/ramen-shops")
                        .param("keyword", "이리에"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("이리에 라멘"));
    }

    @Test
    void hiddenShopIsExcludedFromListAndDetail() throws Exception {
        RamenShop hiddenShop = RamenShop.builder()
                .name("숨김 라멘")
                .address(Address.of("서울", "성동구", "어딘가 2", "1층"))
                .businessHours(BusinessHours.of("일요일", LocalTime.of(11, 0), LocalTime.of(20, 0), null, null, "불가"))
                .tags(List.of("돈코츠"))
                .description("숨김 매장")
                .published(false)
                .normalMenus(NormalMenus.init())
                .eventMenus(EventMenus.init())
                .build();
        RamenShop savedShop = ramenShopRepository.save(hiddenShop);

        mockMvc.perform(get("/ramen-shops").param("city", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));

        mockMvc.perform(get("/ramen-shops/{shopId}", savedShop.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchByViewsSortReturnsViewCountPriorityOrder() throws Exception {
        RamenShop low = sampleShop("로우", "정렬시", "A");
        low.increaseViewCount();

        RamenShop popular = sampleShop("인기", "정렬시", "B");
        popular.increaseViewCount();
        popular.increaseViewCount();
        popular.increaseViewCount();

        RamenShop middle = sampleShop("미들", "정렬시", "C");
        middle.increaseViewCount();
        middle.increaseViewCount();

        ramenShopRepository.saveAll(List.of(low, popular, middle));

        mockMvc.perform(get("/ramen-shops")
                        .param("city", "정렬시")
                        .param("sort", "VIEWS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].name").value("인기"))
                .andExpect(jsonPath("$.data.items[1].name").value("미들"))
                .andExpect(jsonPath("$.data.items[2].name").value("로우"));
    }

    @Test
    void searchByLatestSortWithPagingReturnsOk() throws Exception {
        ramenShopRepository.save(sampleShop("첫번째", "서울", "성동구"));
        ramenShopRepository.save(sampleShop("두번째", "서울", "성동구"));

        mockMvc.perform(get("/ramen-shops")
                        .param("page", "0")
                        .param("size", "12")
                        .param("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void shopListReturnsRamenLogCountAndLatestThreePreviewImages() throws Exception {
        RamenShop shop = ramenShopRepository.save(sampleShop("미리보기 라멘", "미리보기시", "미리보기구"));
        MemberProfile member = memberRepository.save(MemberProfile.builder()
                .nickname("미리보기 작성자")
                .build());

        saveRamenLog(shop, member, "https://example.com/log-1.jpg", true);
        saveRamenLog(shop, member, "https://example.com/log-2.jpg", true);
        saveRamenLog(shop, member, "https://example.com/log-3.jpg", true);
        saveRamenLog(shop, member, "https://example.com/log-4.jpg", true);
        saveRamenLog(shop, member, "https://example.com/private-log.jpg", false);

        mockMvc.perform(get("/ramen-shops")
                        .param("city", "미리보기시"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].ramenLogCount").value(4))
                .andExpect(jsonPath("$.data.items[0].ramenLogPreviewImageUrls.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].ramenLogPreviewImageUrls[0]")
                        .value("https://example.com/log-4.jpg"))
                .andExpect(jsonPath("$.data.items[0].ramenLogPreviewImageUrls[1]")
                        .value("https://example.com/log-3.jpg"))
                .andExpect(jsonPath("$.data.items[0].ramenLogPreviewImageUrls[2]")
                        .value("https://example.com/log-2.jpg"));
    }

    @Test
    void searchByNameSortReturnsAscendingOrder() throws Exception {
        ramenShopRepository.save(sampleShop("카", "서울", "성동구"));
        ramenShopRepository.save(sampleShop("가", "서울", "성동구"));
        ramenShopRepository.save(sampleShop("나", "서울", "성동구"));

        mockMvc.perform(get("/ramen-shops")
                        .param("city", "서울")
                        .param("sort", "NAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].name").value("가"))
                .andExpect(jsonPath("$.data.items[1].name").value("나"))
                .andExpect(jsonPath("$.data.items[2].name").value("카"));
    }

    @Test
    void invalidSortReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/ramen-shops")
                        .param("sort", "name,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청 파라미터입니다: sort"));
    }

    private RamenShop sampleShop(String name, String city, String district) {
        RamenShop ramenShop = RamenShop.builder()
                .name(name)
                .address(Address.of(city, district, "어딘가 1", "1층"))
                .businessHours(BusinessHours.of("일요일", LocalTime.of(11, 0), LocalTime.of(20, 0), null, null, "불가"))
                .tags(List.of("돈코츠"))
                .imageUrl("https://example.com/" + name + ".jpg")
                .instagramUrl("https://instagram.com/" + name)
                .catchTableUrl("https://app.catchtable.co.kr/ct/shop/" + name)
                .description(name + " 설명")
                .normalMenus(NormalMenus.init())
                .eventMenus(EventMenus.init())
                .build();
        ramenShop.addNormalMenu(NormalMenu.builder()
                .name("기본 라멘")
                .price(10000)
                .isSignature(true)
                .imageUrl("https://example.com/" + name + "-menu.jpg")
                .build());
        return ramenShop;
    }

    private void saveRamenLog(RamenShop shop, MemberProfile member, String imageUrl, boolean isPublic) {
        ramenLogRepository.saveAndFlush(RamenLog.builder()
                .ramenShop(shop)
                .author(member)
                .menuName("기본 라멘")
                .imageUrl(imageUrl)
                .isPublic(isPublic)
                .build());
    }
}
