package com.raota.acceptance.presentation.admin.ramenShop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.model.MemberRole;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.ramenshop.domain.model.Address;
import com.raota.ramenshop.domain.model.BusinessHours;
import com.raota.ramenshop.domain.model.EventMenus;
import com.raota.ramenshop.domain.model.NormalMenu;
import com.raota.ramenshop.domain.model.NormalMenus;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.account.infrastructure.auth.JwtTokenProvider;
import com.raota.support.BaseIntegrationTest;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional
class RamenShopAdminControllerTest extends BaseIntegrationTest {

    private MockMvc mockMvc;
    private String adminToken;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        ramenShopRepository.deleteAll();

        MemberProfile admin = memberRepository.saveAndFlush(MemberProfile.builder()
                .nickname("관리자")
                .role(MemberRole.ADMIN)
                .build());
        adminToken = jwtTokenProvider.createAccessToken(admin.getId());
    }

    @Test
    void getShopsWithJsonApi() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("목록 라멘"));

        mockMvc.perform(admin(get("/admin/api/ramen-shops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value(savedShop.getId()))
                .andExpect(jsonPath("$.data[0].name").value("목록 라멘"));
    }

    @Test
    void getShopWithJsonApi() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("상세 라멘"));

        mockMvc.perform(admin(get("/admin/api/ramen-shops/{shopId}", savedShop.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(savedShop.getId()))
                .andExpect(jsonPath("$.data.name").value("상세 라멘"))
                .andExpect(jsonPath("$.data.city").value("서울"))
                .andExpect(jsonPath("$.data.normalMenus[0].name").value("기본 라멘"))
                .andExpect(jsonPath("$.data.eventMenus.length()").value(0));
    }

    @Test
    void createShopWithJsonApi() throws Exception {
        mockMvc.perform(admin(post("/admin/api/ramen-shops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "제이슨 라멘",
                                  "city": "서울",
                                  "district": "마포구",
                                  "street": "동교로 12",
                                  "detail": "1층",
                                  "latitude": "37.12345678",
                                  "longitude": "127.12345678",
                                  "closedDays": "월요일",
                                  "openTime": "11:00",
                                  "closeTime": "21:00",
                                  "instagramUrl": "https://instagram.com/json_ramen",
                                  "description": "JSON으로 등록한 라멘집",
                                  "published": false,
                                  "tags": "쇼유, 혼밥",
                                  "normalMenus": [
                                    { "name": "쇼유 라멘", "price": 12000, "signature": true }
                                  ],
                                  "eventMenus": []
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").exists());

        RamenShop shop = ramenShopRepository.findAll().getFirst();
        assertThat(shop.getName()).isEqualTo("제이슨 라멘");
        assertThat(shop.getAddress().latitude()).isEqualByComparingTo("37.12345678");
        assertThat(shop.getNormalMenus().getValues()).hasSize(1);
        assertThat(shop.isPublished()).isFalse();
    }

    @Test
    void updateShopWithJsonApi() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("수정 전"));

        mockMvc.perform(admin(put("/admin/api/ramen-shops/{shopId}", savedShop.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "JSON 수정 후",
                                  "city": "서울",
                                  "district": "성동구",
                                  "street": "성수이로 7",
                                  "detail": "2층",
                                  "closedDays": "화요일",
                                  "openTime": "10:30",
                                  "closeTime": "20:00",
                                  "parkingInfo": "불가",
                                  "instagramUrl": "https://instagram.com/json_updated",
                                  "description": "JSON으로 수정한 라멘집",
                                  "published": false,
                                  "tags": "츠케멘",
                                  "normalMenus": [
                                    { "name": "츠케멘", "price": 14000, "signature": true }
                                  ],
                                  "eventMenus": [
                                    { "name": "한정 라멘", "price": 16000, "badgeText": "LIMITED", "startDate": "2026-05-01", "endDate": "2026-05-31" }
                                  ]
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(savedShop.getId()));

        RamenShop updatedShop = ramenShopRepository.findById(savedShop.getId()).orElseThrow();
        assertThat(updatedShop.getName()).isEqualTo("JSON 수정 후");
        assertThat(updatedShop.getImageUrl()).isNull();
        assertThat(updatedShop.getNormalMenus().getValues().getFirst().getName()).isEqualTo("츠케멘");
        assertThat(updatedShop.getEventMenus().getValues()).hasSize(1);
        assertThat(updatedShop.isPublished()).isFalse();
    }

    @Test
    void updateShopVisibilityWithJsonApi() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("숨김 전환 대상"));

        mockMvc.perform(admin(patch("/admin/api/ramen-shops/{shopId}/visibility", savedShop.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "published": false
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(savedShop.getId()));

        assertThat(ramenShopRepository.findById(savedShop.getId()).orElseThrow().isPublished()).isFalse();
    }

    @Test
    void updateShopVisibilityByIdRangeWithJsonApi() throws Exception {
        RamenShop first = ramenShopRepository.save(sampleShop("범위 첫 매장"));
        RamenShop second = ramenShopRepository.save(sampleShop("범위 둘째 매장"));

        mockMvc.perform(admin(patch("/admin/api/ramen-shops/visibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromId": %d,
                                  "toId": %d,
                                  "published": false
                                }
                                """.formatted(first.getId(), second.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.updatedCount").value(2));

        assertThat(ramenShopRepository.findById(first.getId()).orElseThrow().isPublished()).isFalse();
        assertThat(ramenShopRepository.findById(second.getId()).orElseThrow().isPublished()).isFalse();
    }

    @Test
    void deleteShopWithJsonApi() throws Exception {
        RamenShop savedShop = ramenShopRepository.save(sampleShop("JSON 삭제 대상"));

        mockMvc.perform(admin(delete("/admin/api/ramen-shops/{shopId}", savedShop.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(savedShop.getId()));

        assertThat(ramenShopRepository.findAll()).isEmpty();
    }

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken);
    }

    private RamenShop sampleShop(String name) {
        RamenShop ramenShop = RamenShop.builder()
                .name(name)
                .address(Address.of("서울", "강남구", "강남대로 1", "1층"))
                .businessHours(BusinessHours.of(
                        "일요일",
                        LocalTime.of(11, 0),
                        LocalTime.of(20, 0),
                        null,
                        null,
                        "불가"
                ))
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
