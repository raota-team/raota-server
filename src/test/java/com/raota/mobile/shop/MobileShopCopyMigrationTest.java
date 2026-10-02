package com.raota.mobile.shop;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.raota.support.BaseIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** 배포 후 같은 복사 SQL을 다시 실행해도 v2에서 수정한 데이터는 유지되어야 한다. */
class MobileShopCopyMigrationTest extends BaseIntegrationTest {

    private static final long PUBLISHED = 900000001L;

    private static final long HIDDEN = 900000002L;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void v1_매장_복사는_누락된_행만_추가하고_기존_수정은_보존한다() throws Exception {
        // 복사 SQL은 v1 테이블 전체를 읽으므로 다른 테스트가 남긴 v1 매장도 v2로 복사된다. 실행 전에 있던 v2 매장만 남긴다.
        List<Long> existingShops = jdbc.queryForList("SELECT id FROM tb_v2_shop", Long.class);
        try {
            jdbc.update("""
                    INSERT INTO tb_ramen_shop (ramen_shop_id, name, branch_name, naver_map_id,
                        city, district, street, detail, latitude, longitude, description, detailed_description,
                        catch_table_url, closed_days, open_time, close_time, break_start, break_end,
                        tags, image_url, view_count, is_published)
                    VALUES (?, '밤 라멘', '본점', 'place-1', '서울', '마포구', '망원로', '  ',
                        37.55000000, 126.90000000, '한 줄', '긴 소개', 'https://reservation', '수요일',
                        '22:00:00', '02:00:00', '23:00:00', '23:30:00',
                        '["쇼우", "쇼유", "톤코츠", "담담멘", "시오", "미소", "츠케멘", "마제소바", "아부라소바"]',
                        'https://image', 17, TRUE)
                    """, PUBLISHED);
            jdbc.update("""
                    INSERT INTO tb_ramen_shop (ramen_shop_id, name, city, district, tags, image_url, is_published)
                    VALUES (?, '숨긴 매장', '부산', ' ', NULL, ' ', FALSE)
                    """, HIDDEN);

            runCopy();
            Map<String, Object> shop = jdbc.queryForMap("SELECT * FROM tb_v2_shop WHERE id = ?", PUBLISHED);
            assertThat(shop).containsEntry("name", "밤 라멘")
                .containsEntry("branch_name", "본점")
                .containsEntry("address", "서울 마포구 망원로")
                .containsEntry("region", "마포구")
                .containsEntry("naver_place_id", "place-1")
                .containsEntry("reservation_url", "https://reservation")
                .containsEntry("tagline", "한 줄")
                .containsEntry("description", "긴 소개")
                .containsEntry("closed_days_text", "수요일")
                .containsEntry("view_count", 17)
                .containsEntry("log_count", 0)
                .containsEntry("bookmark_count", 0)
                .containsEntry("business_status", "OPERATIONAL");
            assertThat(shop.get("hours_verified_at")).isNull();
            assertThat(mapper.readValue(shop.get("ramen_types").toString(), new TypeReference<List<String>>() {
            })).containsExactlyInAnyOrder("쇼유", "돈코츠", "탄탄멘", "시오", "미소", "츠케멘", "마제소바", "아부라소바");
            assertThat(jdbc.queryForObject("SELECT business_status FROM tb_v2_shop WHERE id = ?", String.class, HIDDEN))
                .isEqualTo("UNKNOWN");
            assertThat(jdbc.queryForObject("SELECT region FROM tb_v2_shop WHERE id = ?", String.class, HIDDEN))
                .isEqualTo("부산");
            assertThat(jdbc.queryForObject("SELECT ramen_types FROM tb_v2_shop WHERE id = ?", String.class, HIDDEN))
                .isEqualTo("[]");
            assertThat(jdbc.queryForObject("SELECT tags FROM tb_v2_shop WHERE id = ?", String.class, HIDDEN))
                .isEqualTo("[]");
            assertThat(jdbc.queryForObject("SELECT is_published FROM tb_v2_shop WHERE id = ?", Boolean.class, HIDDEN))
                .isFalse();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_image WHERE shop_id = ?", Integer.class,
                    PUBLISHED))
                .isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT source FROM tb_v2_shop_image WHERE shop_id = ?", String.class,
                    PUBLISHED))
                .isEqualTo("ADMIN");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_image WHERE shop_id = ?", Integer.class,
                    HIDDEN))
                .isZero();
            assertThat(jdbc.queryForList("""
                    SELECT day_of_week FROM tb_v2_shop_business_hour WHERE shop_id = ? ORDER BY day_of_week
                    """, Integer.class, PUBLISHED)).containsExactly(1, 2, 3, 4, 5, 6, 7);
            assertThat(jdbc.queryForObject("""
                    SELECT TIME_FORMAT(closes_at, '%H:%i') FROM tb_v2_shop_business_hour
                    WHERE shop_id = ? AND day_of_week = 1
                    """, String.class, PUBLISHED)).isEqualTo("02:00");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_business_hour WHERE shop_id = ?",
                    Integer.class, HIDDEN))
                .isZero();

            jdbc.update("UPDATE tb_v2_shop SET name = 'v2 수정', business_status = 'CLOSED' WHERE id = ?", PUBLISHED);
            runCopy();
            assertThat(jdbc.queryForObject("SELECT name FROM tb_v2_shop WHERE id = ?", String.class, PUBLISHED))
                .isEqualTo("v2 수정");
            assertThat(
                    jdbc.queryForObject("SELECT business_status FROM tb_v2_shop WHERE id = ?", String.class, PUBLISHED))
                .isEqualTo("CLOSED");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop WHERE id IN (?, ?)", Integer.class,
                    PUBLISHED, HIDDEN))
                .isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_image WHERE shop_id IN (?, ?)",
                    Integer.class, PUBLISHED, HIDDEN))
                .isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_business_hour WHERE shop_id IN (?, ?)",
                    Integer.class, PUBLISHED, HIDDEN))
                .isEqualTo(7);
        }
        finally {
            jdbc.update("DELETE FROM tb_ramen_shop WHERE ramen_shop_id IN (?, ?)", PUBLISHED, HIDDEN);
            List<Long> copiedShops = jdbc.queryForList("SELECT id FROM tb_v2_shop", Long.class)
                .stream()
                .filter(id -> !existingShops.contains(id))
                .toList();
            for (Long shopId : copiedShops) {
                for (String table : List.of("tb_v2_shop_image", "tb_v2_shop_business_hour")) {
                    jdbc.update("DELETE FROM " + table + " WHERE shop_id = ?", shopId);
                }
                jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", shopId);
            }
        }
    }

    private void runCopy() {
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V29__copy_v1_shops_to_v2.sql"))
            .execute(dataSource);
    }

}
