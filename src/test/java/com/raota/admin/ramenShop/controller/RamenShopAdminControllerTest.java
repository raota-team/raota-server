package com.raota.admin.ramenShop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
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
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "shop.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/admin/ramen-shops")
                        .file(imageFile)
                        .param("name", "신규 라멘")
                        .param("city", "서울")
                        .param("district", "성동구")
                        .param("street", "연무장길 10")
                        .param("detail", "2층")
                        .param("closedDays", "월요일")
                        .param("openTime", "11:00")
                        .param("closeTime", "20:30")
                        .param("instagramUrl", "https://instagram.com/new-ramen")
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
        assertThat(shops.getFirst().getImageUrl()).isEqualTo("https://mock.cdn.com/uploaded/702.jpg");
        assertThat(shops.getFirst().getNormalMenus().getValues()).hasSize(1);
        assertThat(shops.getFirst().getEventMenus().getValues()).hasSize(1);
    }

    @Test
    void createShopWithoutImageStoresNull() throws Exception {
        mockMvc.perform(multipart("/admin/ramen-shops")
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
    void updateShopWithoutImageClearsImageUrl() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("수정 전"));

        mockMvc.perform(multipart("/admin/ramen-shops/{shopId}", savedShop.getId())
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
                        .param("instagramUrl", "https://instagram.com/updated-shop")
                        .param("tags", "츠케멘, 진한국물")
                        .param("normalMenus[0].name", "츠케멘")
                        .param("normalMenus[0].price", "13000")
                        .param("eventMenus[0].name", "콜라보 라멘")
                        .param("eventMenus[0].price", "15000")
                        .param("eventMenus[0].badgeText", "COLLAB")
                        .param("eventMenus[0].description", "특제 토핑 포함")
                        .param("eventMenus[0].startDate", "2026-03-01")
                        .param("eventMenus[0].endDate", "2026-03-31")
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/ramen-shops?shopId=*"));

        RamenShop updatedShop = ramenShopRepository.findById(savedShop.getId()).orElseThrow();
        assertThat(updatedShop.getName()).isEqualTo("수정 후");
        assertThat(updatedShop.getAddress().district()).isEqualTo("마포구");
        assertThat(updatedShop.getImageUrl()).isNull();
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
                .businessHours(BusinessHours.of("일요일", LocalTime.of(11, 0), LocalTime.of(20, 0), null, null))
                .tags(List.of("돈코츠"))
                .imageUrl("https://example.com/original.jpg")
                .instagramUrl("https://instagram.com/original")
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
