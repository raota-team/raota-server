package com.raota.domain.ramenShop.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.BusinessHours;
import com.raota.domain.ramenShop.model.EventMenus;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.NormalMenus;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;



import com.raota.testsupport.BaseIntegrationTest;

@Transactional
class RamenShopInfoControllerTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
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
                .andExpect(jsonPath("$.data.items[0].name").value("멘야 하쿠"))
                .andExpect(jsonPath("$.data.items[0].tagLine").value("진한 국물 맛집")) // 설명이 한줄평으로 나오는지 확인
                .andExpect(jsonPath("$.data.items[0].tags").value(hasItems("토리파이탄", "혼밥"))) // 태그 확인
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
    void searchByKeywordReturnsMatchingItemsOnly() throws Exception {
        ramenShopRepository.save(sampleShop("멘야 하쿠", "서울", "성동구"));
        ramenShopRepository.save(sampleShop("이리에 라멘", "서울", "마포구"));

        mockMvc.perform(get("/ramen-shops")
                        .param("keyword", "이리에"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("이리에 라멘"));
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
                .build());
        return ramenShop;
    }
}
