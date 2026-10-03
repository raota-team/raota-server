package com.raota.mobile.shop.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.common.cursor.Cursor;
import com.raota.support.BaseIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 매장 목록의 SQL 정렬, 공개 범위, 페이지 연결과 앱 응답 형식을 검증한다. */
class MobileShopQueryIntegrationTest extends BaseIntegrationTest {

    private static final long FIRST = 900002101L;

    private static final long SECOND = 900002102L;

    private static final long THIRD = 900002103L;

    private static final long HIDDEN = 900002104L;

    private static final long DELETED = 900002105L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        seed(FIRST, "쇼유집", "마포구", "[\"쇼유\",\"시오\"]", 10, "2026-09-01T00:00:00Z", "37.00000000", true, false, true);
        seed(SECOND, "미소집", "성동구", "[\"미소\"]", 5, "2026-09-03T00:00:00Z", "37.01000000", true, false, false);
        seed(THIRD, "쇼유 분점", "마포구", "[\"쇼유\"]", 5, "2026-09-02T00:00:00Z", "37.02000000", true, false, true);
        seed(HIDDEN, "비공개", "마포구", "[]", 100, "2026-09-04T00:00:00Z", "37.00000000", false, false, false);
        seed(DELETED, "삭제됨", "마포구", "[]", 100, "2026-09-05T00:00:00Z", "37.00000000", true, true, false);
        jdbc.update("""
                INSERT INTO tb_v2_shop_image (shop_id, url, source, sort_order) VALUES
                    (?, 'https://later', 'ADMIN', 5), (?, 'https://first', 'ADMIN', 0)
                """, FIRST, FIRST);
        jdbc.update("""
                INSERT INTO tb_v2_shop_business_hour (shop_id, day_of_week, opens_at, closes_at, is_closed)
                VALUES (?, 1, '00:00:00', '00:00:00', FALSE), (?, 1, '00:00:00', '00:00:00', TRUE)
                """, FIRST, THIRD);
        for (int day = 2; day <= 7; day++) {
            jdbc.update("""
                    INSERT INTO tb_v2_shop_business_hour (shop_id, day_of_week, opens_at, closes_at, is_closed)
                    VALUES (?, ?, '00:00:00', '00:00:00', FALSE), (?, ?, '00:00:00', '00:00:00', TRUE)
                    """, FIRST, day, THIRD, day);
        }
    }

    @AfterEach
    void cleanUp() {
        for (String table : List.of("tb_v2_shop_image", "tb_v2_shop_business_hour", "tb_v2_shop")) {
            jdbc.update("DELETE FROM " + table + " WHERE " + (table.equals("tb_v2_shop") ? "id" : "shop_id")
                    + " BETWEEN ? AND ?", FIRST, DELETED);
        }
    }

    @Test
    void 공개_매장만_목록과_지도에_나오며_이미지와_빈_필드가_계약대로_표시된다() throws Exception {
        JsonNode page = list("/api/v2/shops");
        assertThat(ids(page)).containsExactly(id(FIRST), id(THIRD), id(SECOND));
        JsonNode first = page.get("items").get(0);
        assertThat(first.get("imageUrl").asString()).isEqualTo("https://first");
        assertThat(first.get("id").isString()).isTrue();
        assertThat(first.get("isBookmarked").asBoolean()).isFalse();
        assertThat(first.get("distanceMeters").isNull()).isTrue();
        assertThat(first.get("isOpen").asBoolean()).isTrue();
        assertThat(page.get("items").get(2).get("isOpen").isNull()).isTrue();
        assertThat(page.get("items").get(2).get("imageUrl").isNull()).isTrue();

        JsonNode pins = data("/api/v2/shops/map-pins");
        assertThat(ids(pins)).containsExactlyInAnyOrder(id(FIRST), id(SECOND), id(THIRD));
        assertThat(pins.get(0).get("latitude").isNumber()).isTrue();
        assertThat(mapper.readValue(first.toString(),
                new tools.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                }))
            .containsOnlyKeys("id", "name", "branchName", "address", "region", "latitude", "longitude", "imageUrl",
                    "tagline", "ramenTypes", "tags", "logCount", "bookmarkCount", "isBookmarked", "businessStatus",
                    "isOpen", "distanceMeters");
        assertThat(mapper.readValue(pins.get(0).toString(),
                new tools.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                }))
            .containsOnlyKeys("id", "name", "branchName", "latitude", "longitude", "ramenTypes", "isOpen");
    }

    @Test
    void 인기_최신_거리순_커서_페이지는_누락이나_중복이_없다() throws Exception {
        assertPages("POPULAR", List.of(id(FIRST), id(THIRD), id(SECOND)), "");
        assertPages("LATEST", List.of(id(SECOND), id(THIRD), id(FIRST)), "");
        assertPages("DISTANCE", List.of(id(FIRST), id(SECOND), id(THIRD)),
                "&latitude=37.00000000&longitude=127.00000000");
        JsonNode distance = list("/api/v2/shops?sort=DISTANCE&latitude=37&longitude=127");
        assertThat(distance.get("items").get(0).get("distanceMeters").asInt()).isZero();
        assertThat(distance.get("items").get(1).get("distanceMeters").asInt()).isGreaterThan(1000);
    }

    @Test
    void 페이지_크기와_거리순_좌표가_잘못되면_400이다() throws Exception {
        for (String params : List.of("size=0", "size=51", "sort=DISTANCE", "sort=DISTANCE&latitude=37",
                "sort=DISTANCE&latitude=91&longitude=127", "sort=DISTANCE&latitude=1E-999999999&longitude=0",
                "sort=DISTANCE&latitude=37&longitude=127&cursor=" + new Cursor("1E-999999999", 1).encode())) {
            mvc.perform(get("/api/v2/shops?" + params))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void 영업중_필터는_검증된_실제_영업_매장만_포함한다() throws Exception {
        assertThat(ids(list("/api/v2/shops?openNow=true"))).containsExactly(id(FIRST));
    }

    @Test
    void 이름_지역_라멘_종류는_각각_검색된다() throws Exception {
        assertThat(ids(listWith("query", "쇼유"))).containsExactly(id(FIRST), id(THIRD));
        assertThat(ids(listWith("region", "성동구"))).containsExactly(id(SECOND));
        assertThat(ids(listWith("ramenType", "미소"))).containsExactly(id(SECOND));
    }

    @Test
    void 공개_조회에_무효한_토큰을_보내면_v2_401이다() throws Exception {
        mvc.perform(get("/api/v2/shops").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 공개_상세는_사진과_시간과_혜택을_내보내고_조회수를_원자적으로_늘린다() throws Exception {
        jdbc.update("""
                UPDATE tb_v2_shop SET phone = '02-1234', instagram_url = 'https://instagram',
                    reservation_url = 'https://reserve', website_url = 'https://website',
                    naver_place_id = 'naver-1', kakao_place_id = 'kakao-1',
                    price_min = 9000, price_max = 14000, description = '긴 소개', tagline = '한 줄',
                    closed_days_text = '수요일', ai_review_summary = '육수가 진하다',
                    ai_summary_keywords = '["육수","면"]',
                    ai_summary_generated_at = '2026-09-01 01:00:00.123456'
                WHERE id = ?
                """, FIRST);
        jdbc.update("""
                INSERT INTO tb_v2_shop_service_perk
                    (shop_id, perk_type, status, price, condition_text, verified_at)
                VALUES (?, 'NOODLE_REFILL', 'FREE', NULL, '점심', '2026-09-01 01:00:00.123456')
                """, FIRST);

        JsonNode detail = data("/api/v2/shops/" + FIRST);
        assertThat(detail.get("id").asString()).isEqualTo(id(FIRST));
        assertThat(detail.get("description").asString()).isEqualTo("긴 소개");
        assertThat(detail.get("phone").asString()).isEqualTo("02-1234");
        assertThat(detail.get("images").get(0).get("url").asString()).isEqualTo("https://first");
        assertThat(detail.get("images").get(1).get("url").asString()).isEqualTo("https://later");
        assertThat(detail.get("businessHours").size()).isEqualTo(7);
        assertThat(detail.get("businessHours").get(0).get("dayOfWeek").asInt()).isEqualTo(1);
        assertThat(detail.get("businessHours").get(0).get("opensAt").asString()).isEqualTo("00:00");
        assertThat(detail.get("businessHours").get(0).get("lastOrderAt").isNull()).isTrue();
        assertThat(detail.get("servicePerks").get(0).get("type").asString()).isEqualTo("NOODLE_REFILL");
        assertThat(detail.get("servicePerks").get(0).get("status").asString()).isEqualTo("FREE");
        assertThat(detail.get("aiSummaryKeywords").get(0).asString()).isEqualTo("육수");
        assertThat(detail.get("hoursVerifiedAt").asString()).endsWith("Z");
        assertThat(detail.get("aiSummaryGeneratedAt").asString()).contains(".123456Z");
        assertThat(mapper.readValue(detail.toString(),
                new tools.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                }))
            .containsOnlyKeys("id", "name", "branchName", "address", "region", "latitude", "longitude", "imageUrl",
                    "tagline", "ramenTypes", "tags", "logCount", "bookmarkCount", "averageSatisfaction", "isBookmarked",
                    "businessStatus", "isOpen", "distanceMeters", "description", "phone", "instagramUrl",
                    "reservationUrl", "websiteUrl", "naverPlaceId", "kakaoPlaceId", "priceMin", "priceMax",
                    "closedDaysText", "hoursVerifiedAt", "images", "businessHours", "servicePerks", "aiReviewSummary",
                    "aiSummaryKeywords", "aiSummaryGeneratedAt");

        data("/api/v2/shops/" + FIRST);
        assertThat(jdbc.queryForObject("SELECT view_count FROM tb_v2_shop WHERE id = ?", Integer.class, FIRST))
            .isEqualTo(12);
    }

    @Test
    void 알_수_없거나_숨김_삭제된_매장_상세는_404이다() throws Exception {
        for (long id : new long[] { HIDDEN, DELETED, 900002999L }) {
            mvc.perform(get("/api/v2/shops/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    private void assertPages(String sort, List<String> expected, String coordinates) throws Exception {
        List<String> received = new ArrayList<>();
        String cursor = null;
        for (int i = 0; i < expected.size(); i++) {
            String path = "/api/v2/shops?sort=" + sort + "&size=1" + coordinates
                    + (cursor == null ? "" : "&cursor=" + cursor);
            JsonNode page = list(path);
            received.addAll(ids(page));
            assertThat(page.get("hasNext").asBoolean()).isEqualTo(i < expected.size() - 1);
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asString();
        }
        assertThat(received).containsExactlyElementsOf(expected);
    }

    private JsonNode list(String path) throws Exception {
        return data(path);
    }

    private JsonNode listWith(String name, String value) throws Exception {
        byte[] body = mvc.perform(get("/api/v2/shops").param(name, value))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        return mapper.readTree(body).get("data");
    }

    private JsonNode data(String path) throws Exception {
        byte[] body = mvc.perform(get(path))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        return mapper.readTree(body).get("data");
    }

    private List<String> ids(JsonNode entries) {
        JsonNode nodes = entries.isArray() ? entries : entries.get("items");
        List<String> ids = new ArrayList<>();
        nodes.forEach(node -> ids.add(node.get("id").asString()));
        return ids;
    }

    private String id(long value) {
        return Long.toString(value);
    }

    private void seed(long id, String name, String region, String ramenTypes, int views, String createdAt,
            String latitude, boolean published, boolean deleted, boolean verified) {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, latitude, longitude,
                    ramen_types, tags, business_status, hours_verified_at, ai_summary_keywords,
                    view_count, log_count, bookmark_count, is_published, created_at, updated_at, deleted_at)
                VALUES (?, ?, NULL, '서울', ?, ?, 127.00000000, ?, '[]', 'OPERATIONAL', ?, '[]',
                    ?, 0, 0, ?, ?, ?, ?)
                """, id, name, region, latitude, ramenTypes,
                verified ? Timestamp.from(Instant.parse("2026-09-01T00:00:00Z")) : null, views, published,
                Timestamp.from(Instant.parse(createdAt)), Timestamp.from(Instant.parse(createdAt)),
                deleted ? Timestamp.from(Instant.parse("2026-09-06T00:00:00Z")) : null);
    }

}
