package com.raota.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RamenShopAdminControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        ramenShopRepository.deleteAll();
    }

    @Test
    void adminPageRenders() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("멘야 하쿠"));

        mockMvc.perform(get("/admin/ramen-shops").param("shopId", savedShop.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ramen-shops"));
    }

    @Test
    void createShop() throws Exception {
        mockMvc.perform(post("/admin/ramen-shops")
                        .param("name", "신규 라멘")
                        .param("city", "서울")
                        .param("district", "성동구")
                        .param("street", "연무장길 10")
                        .param("detail", "2층")
                        .param("closedDays", "월요일")
                        .param("openTime", "11:00")
                        .param("closeTime", "20:30")
                        .param("parkingInfo", "불가")
                        .param("instagramUrl", "https://instagram.com/new-ramen")
                        .param("catchTableUrl", "https://app.catchtable.co.kr/ct/shop/new-ramen")
                        .param("description", "진한 돈코츠 국물과 차슈가 강점인 가게")
                        .param("imageUrl", "https://mock.cdn.com/ramen-shop/test.jpg")
                        .param("tags", "진한국물, 혼밥")
                        .param("normalMenus[0].name", "돈코츠 라멘")
                        .param("normalMenus[0].price", "11000")
                        .param("normalMenus[0].signature", "true")
                        .param("eventMenus[0].name", "봄 한정 라멘")
                        .param("eventMenus[0].price", "14000")
                        .param("eventMenus[0].badgeText", "SPRING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/ramen-shops?shopId=*"));

        List<RamenShop> shops = ramenShopRepository.findAll();
        assertThat(shops).hasSize(1);
        assertThat(shops.getFirst().getName()).isEqualTo("신규 라멘");
        assertThat(shops.getFirst().getImageUrl()).isEqualTo("https://mock.cdn.com/ramen-shop/test.jpg");
        assertThat(shops.getFirst().getCatchTableUrl()).isEqualTo("https://app.catchtable.co.kr/ct/shop/new-ramen");
        assertThat(shops.getFirst().getDescription()).isEqualTo("진한 돈코츠 국물과 차슈가 강점인 가게");
        assertThat(shops.getFirst().getBusinessHours().parkingInfo()).isEqualTo("불가");
        assertThat(shops.getFirst().getNormalMenus().getValues()).hasSize(1);
        assertThat(shops.getFirst().getEventMenus().getValues()).hasSize(1);
    }

    @Test
    void createShopWithoutImageStoresNull() throws Exception {
        mockMvc.perform(post("/admin/ramen-shops")
                        .param("name", "이미지 없는 라멘")
                        .param("city", "서울")
                        .param("district", "성동구")
                        .param("street", "연무장길 10")
                        .param("detail", "2층")
                        .param("normalMenus[0].name", "쇼유 라멘")
                        .param("normalMenus[0].price", "10000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/ramen-shops?shopId=*"));

        RamenShop shop = ramenShopRepository.findAll().getFirst();
        assertThat(shop.getImageUrl()).isNull();
    }

    @Test
    void updateShopWithoutImageKeepsImageUrl() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("수정 전"));

        mockMvc.perform(post("/admin/ramen-shops/{shopId}", savedShop.getId())
                        .param("name", "수정 후")
                        .param("city", "서울")
                        .param("district", "마포구")
                        .param("street", "독막로 88")
                        .param("detail", "1층")
                        .param("closedDays", "화요일")
                        .param("openTime", "10:30")
                        .param("closeTime", "21:00")
                        .param("breakStart", "15:00")
                        .param("breakEnd", "17:00")
                        .param("parkingInfo", "매장 앞 1대 가능")
                        .param("instagramUrl", "https://instagram.com/updated-shop")
                        .param("catchTableUrl", "https://app.catchtable.co.kr/ct/shop/updated-shop")
                        .param("description", "츠케멘과 한정 메뉴 구성이 강한 라멘집")
                        .param("tags", "츠케멘, 진한국물")
                        .param("normalMenus[0].name", "츠케멘")
                        .param("normalMenus[0].price", "13000")
                        .param("eventMenus[0].name", "콜라보 라멘")
                        .param("eventMenus[0].price", "15000")
                        .param("eventMenus[0].badgeText", "COLLAB")
                        .param("eventMenus[0].description", "특제 토핑 포함")
                        .param("eventMenus[0].startDate", "2026-03-01")
                        .param("eventMenus[0].endDate", "2026-03-31")
                        .param("imageUrl", "https://example.com/original.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/ramen-shops?shopId=*"));

        RamenShop updatedShop = ramenShopRepository.findById(savedShop.getId()).orElseThrow();
        assertThat(updatedShop.getName()).isEqualTo("수정 후");
        assertThat(updatedShop.getAddress().district()).isEqualTo("마포구");
        assertThat(updatedShop.getImageUrl()).isEqualTo("https://example.com/original.jpg");
        assertThat(updatedShop.getCatchTableUrl()).isEqualTo("https://app.catchtable.co.kr/ct/shop/updated-shop");
        assertThat(updatedShop.getDescription()).isEqualTo("츠케멘과 한정 메뉴 구성이 강한 라멘집");
        assertThat(updatedShop.getBusinessHours().parkingInfo()).isEqualTo("매장 앞 1대 가능");
        assertThat(updatedShop.getNormalMenus().getValues()).hasSize(1);
        assertThat(updatedShop.getNormalMenus().getValues().getFirst().getName()).isEqualTo("츠케멘");
        assertThat(updatedShop.getEventMenus().getValues()).hasSize(1);
        assertThat(updatedShop.getEventMenus().getValues().getFirst().getBadgeText()).isEqualTo("COLLAB");
    }

    @Test
    void deleteShop() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("삭제 대상"));

        mockMvc.perform(post("/admin/ramen-shops/{shopId}/delete", savedShop.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ramen-shops"));

        assertThat(ramenShopRepository.findAll()).isEmpty();
    }

    private RamenShop sampleShop(String name) {
        RamenShop ramenShop = RamenShop.builder()
                .name(name)
                .address(Address.of("서울", "강남구", "강남대로 1", "1층"))
                .businessHours(BusinessHours.of("일요일", LocalTime.of(11, 0), LocalTime.of(20, 0), null, null, "불가"))
                .tags(List.of("돈코츠"))
                .imageUrl("https://example.com/original.jpg")
                .instagramUrl("https://instagram.com/original")
                .catchTableUrl("https://app.catchtable.co.kr/ct/shop/original")
                .description("기본기가 좋은 정통 돈코츠 라멘집")
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
