package com.raota.mobile.shop.presentation.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.support.BaseIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/** 관리자 웹의 앱 매장 편집: 숨김 생성, 공개 전환, 통째 교체와 검증, 권한을 확인한다. */
class MobileShopAdminIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/admin/api/v2/shops";

    private static final long EXISTING = 900006001L;

    private static final long DELETED = 900006002L;

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, view_count, log_count, bookmark_count, is_published, created_at, updated_at,
                    deleted_at)
                VALUES (?, '관리자테스트 라멘', '본점', '서울 마포구 망원로 1', '서울 마포구', '["쇼유"]', '[]', 'OPERATIONAL',
                    '[]', 30, 7, 4, TRUE, '2026-09-01 00:00:00', '2026-09-01 00:00:00', NULL),
                    (?, '관리자테스트 삭제됨', NULL, '서울 마포구', NULL, '[]', '[]', 'UNKNOWN', '[]', 0, 0, 0, TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00', '2026-09-02 00:00:00')
                """, EXISTING, DELETED);
        jdbc.update("""
                INSERT INTO tb_v2_shop_image (shop_id, url, source, sort_order)
                VALUES (?, 'https://images.raota.net/user.jpg', 'USER', 0)
                """, EXISTING);
        jdbc.update("""
                INSERT INTO tb_v2_shop_business_hour (shop_id, day_of_week, opens_at, closes_at, is_closed)
                VALUES (?, 1, '11:00:00', '21:00:00', FALSE)
                """, EXISTING);
    }

    @AfterEach
    void cleanUp() {
        List<Long> ids = new ArrayList<>(created);
        ids.add(EXISTING);
        ids.add(DELETED);
        for (Long id : ids) {
            jdbc.update("DELETE FROM tb_v2_shop_image WHERE shop_id = ?", id);
            jdbc.update("DELETE FROM tb_v2_shop_business_hour WHERE shop_id = ?", id);
            jdbc.update("DELETE FROM tb_v2_shop_service_perk WHERE shop_id = ?", id);
            jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", id);
        }
    }

    @Test
    void 새_매장은_v1과_겹치지_않는_번호의_숨김_매장으로_만들고_공개하면_앱에_보인다() throws Exception {
        String body = mvc.perform(json(
                                post(BASE),
                                form(
                                        "새 라멘집",
                                        List.of("https://images.raota.net/a.jpg", "https://images.raota.net/b.jpg")))
                        .with(ADMIN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.published").value(false))
                .andExpect(jsonPath("$.data.ramenTypes[0]").value("쇼유"))
                .andExpect(jsonPath("$.data.tags.length()").value(1))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("https://images.raota.net/a.jpg"))
                .andExpect(jsonPath("$.data.branchName").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = Long.parseLong(mapper.readTree(body).path("data").path("id").asString());
        created.add(id);
        assertThat(id).isGreaterThanOrEqualTo(10_000_000L);

        mvc.perform(get("/api/v2/shops/" + id)).andExpect(status().isNotFound());
        mvc.perform(json(patch(BASE + "/" + id + "/visibility"), "{\"published\":true}")
                        .with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(true));
        mvc.perform(get("/api/v2/shops/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새 라멘집"));
    }

    @Test
    void 수정은_집계를_건드리지_않고_남은_사진의_등록_주체를_유지한다() throws Exception {
        mvc.perform(json(
                                put(BASE + "/" + EXISTING),
                                form(
                                        "이름 바꿈",
                                        List.of(
                                                "https://images.raota.net/new.jpg",
                                                "https://images.raota.net/user.jpg")))
                        .with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("이름 바꿈"))
                .andExpect(jsonPath("$.data.logCount").value(7))
                .andExpect(jsonPath("$.data.viewCount").value(30))
                .andExpect(jsonPath("$.data.published").value(true));

        assertThat(jdbc.queryForList(
                        "SELECT source FROM tb_v2_shop_image WHERE shop_id = ? ORDER BY sort_order",
                        String.class,
                        EXISTING))
                .containsExactly("ADMIN", "USER");
        assertThat(jdbc.queryForObject("SELECT bookmark_count FROM tb_v2_shop WHERE id = ?", Integer.class, EXISTING))
                .isEqualTo(4);
    }

    @Test
    void 영업시간은_통째로_바꾸고_확인_시각을_남긴다() throws Exception {
        mvc.perform(json(put(BASE + "/" + EXISTING + "/business-hours"), """
                {"hours":[
                  {"dayOfWeek":2,"closed":false,"opensAt":"11:30","closesAt":"01:00","breakStart":"15:00",
                   "breakEnd":"17:00","lastOrderAt":"00:30"},
                  {"dayOfWeek":1,"closed":true}
                ]}
                """).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hoursVerifiedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.businessHours.length()").value(2))
                .andExpect(jsonPath("$.data.businessHours[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.data.businessHours[0].closed").value(true))
                .andExpect(jsonPath("$.data.businessHours[0].opensAt").isEmpty())
                .andExpect(jsonPath("$.data.businessHours[1].opensAt").value("11:30"))
                .andExpect(jsonPath("$.data.businessHours[1].closesAt").value("01:00"));
    }

    @Test
    void 영업시간이_맞지_않으면_400이고_기존_값은_그대로다() throws Exception {
        String hours = BASE + "/" + EXISTING + "/business-hours";
        mvc.perform(json(put(hours), """
                {"hours":[{"dayOfWeek":1,"closed":true,"opensAt":"11:00"}]}
                """).with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mvc.perform(json(put(hours), """
                {"hours":[{"dayOfWeek":3,"closed":false,"opensAt":"11:00","closesAt":"20:00"},
                          {"dayOfWeek":3,"closed":true}]}
                """).with(ADMIN)).andExpect(status().isBadRequest());
        mvc.perform(json(put(hours), """
                {"hours":[{"dayOfWeek":8,"closed":true}]}
                """).with(ADMIN)).andExpect(status().isBadRequest());
        mvc.perform(json(put(hours), """
                {"hours":[{"dayOfWeek":2,"closed":false,"opensAt":"11:00"}]}
                """).with(ADMIN)).andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_shop_business_hour WHERE shop_id = ?", Integer.class, EXISTING))
                .isEqualTo(1);
    }

    @Test
    void 혜택은_통째로_바꾸고_유료가_아닌_혜택의_가격은_거절한다() throws Exception {
        String perks = BASE + "/" + EXISTING + "/service-perks";
        mvc.perform(json(put(perks), """
                {"perks":[{"type":"NOODLE_REFILL","status":"FREE","price":500}]}
                """).with(ADMIN)).andExpect(status().isBadRequest());

        mvc.perform(json(put(perks), """
                {"perks":[{"type":"NOODLE_REFILL","status":"FREE","condition":"1회 무료"},
                          {"type":"RICE_REFILL","status":"PAID","price":1000}]}
                """).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.servicePerks.length()").value(2))
                .andExpect(jsonPath("$.data.servicePerks[0].condition").value("1회 무료"))
                .andExpect(jsonPath("$.data.servicePerks[1].price").value(1000))
                .andExpect(jsonPath("$.data.servicePerks[1].verifiedAt").isNotEmpty());

        mvc.perform(json(put(perks), "{\"perks\":[]}").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.servicePerks.length()").value(0));
    }

    @Test
    void 목록은_숨김을_포함하고_삭제된_매장은_빼며_검색과_공개_여부로_거른다() throws Exception {
        jdbc.update("UPDATE tb_v2_shop SET is_published = FALSE WHERE id = ?", EXISTING);

        mvc.perform(get(BASE).param("keyword", "관리자테스트").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(EXISTING)))
                .andExpect(jsonPath("$.data.items[0].published").value(false))
                .andExpect(jsonPath("$.data.items[0].thumbnailUrl").value("https://images.raota.net/user.jpg"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.hasNext").value(false));
        mvc.perform(get(BASE)
                        .param("keyword", "관리자테스트")
                        .param("published", "true")
                        .with(ADMIN))
                .andExpect(jsonPath("$.data.items.length()").value(0));
        mvc.perform(get(BASE + "/" + DELETED).with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void 좌표가_한쪽만_있거나_URL이_아니면_400이다() throws Exception {
        String halfCoordinate = form("이름", List.of()).replace("\"latitude\":null", "\"latitude\":37.5");
        mvc.perform(json(post(BASE), halfCoordinate).with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mvc.perform(json(post(BASE), form("이름", List.of("ftp://bad"))).with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop WHERE name = '이름'", Integer.class))
                .isZero();
    }

    @Test
    void 웹_관리자만_쓸_수_있다() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).with(user("member").roles("USER"))).andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String form(String name, List<String> imageUrls) {
        StringBuilder images = new StringBuilder();
        for (String url : imageUrls) {
            if (!images.isEmpty()) {
                images.append(',');
            }
            images.append('"').append(url).append('"');
        }
        return """
                {"name":"%s","branchName":"  ","address":"서울 마포구 망원로 2","region":"서울 마포구",
                 "latitude":null,"longitude":null,"phone":"02-000-0000","tagline":null,"description":null,
                 "ramenTypes":["쇼유"," 쇼유 ",""],"tags":["혼밥"],"instagramUrl":"https://instagram.com/raota",
                 "reservationUrl":"","websiteUrl":null,"naverPlaceId":"123","kakaoPlaceId":null,
                 "priceMin":9000,"priceMax":12000,"businessStatus":"OPERATIONAL","closedDaysText":"매주 월요일",
                 "imageUrls":[%s]}
                """.formatted(name, images);
    }
}
