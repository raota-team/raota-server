package com.raota.mobile.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.support.BaseIntegrationTest;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** '기타'는 k-ramen styles로 실제 종류를 알 수 있을 때만 빼고, 앱 종류가 아닌 메뉴가 있는 매장은 남겨야 한다. */
class MobileShopRamenTypeSplitMigrationTest extends BaseIntegrationTest {

    private static final long FIRST = 900004101L;

    private static final long LAST = 900004108L;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper mapper;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop WHERE id BETWEEN ? AND ?", FIRST, LAST);
    }

    @Test
    void 기타를_k_ramen의_실제_종류로_나누고_다시_실행해도_같다() {
        // k-ramen styles: 토리파이탄
        insertShop(FIRST, "2044271487", "[\"기타\"]", false);
        // k-ramen styles: 시오라멘, 토리파이탄, 아부라소바 (V40이 넣은 값)
        insertShop(FIRST + 1, "1551510195", "[\"시오\", \"기타\", \"아부라소바\"]", false);
        // V30이 '을지로' 태그로 기타를 붙인 매장. k-ramen styles: 지로라멘
        insertShop(FIRST + 2, "1169109563", "[\"기타\"]", false);
        // V30이 츄카소바 태그로 기타를 붙인 매장. k-ramen styles: 쇼유라멘
        insertShop(FIRST + 3, "2095097804", "[\"기타\"]", false);
        // 기타가 없어도 새 종류는 덧붙인다. k-ramen styles: 니보시라멘
        insertShop(FIRST + 4, "2029565676", "[\"시오\"]", false);
        // k-ramen에 앱 종류가 아닌 메뉴가 있으면 기타를 남긴다.
        insertShop(FIRST + 5, "1736892221", "[\"마제소바\", \"기타\"]", false);
        insertShop(FIRST + 6, "no-such-place", "[\"기타\"]", false);
        insertShop(FIRST + 7, "2044271487", "[\"기타\"]", true);

        runSplit();
        assertTypes();
        runSplit();
        assertTypes();
    }

    private void assertTypes() {
        assertThat(types(FIRST)).containsExactly("토리파이탄");
        assertThat(types(FIRST + 1)).containsExactly("시오", "아부라소바", "토리파이탄");
        assertThat(types(FIRST + 2)).containsExactly("지로");
        assertThat(types(FIRST + 3)).containsExactly("쇼유");
        assertThat(types(FIRST + 4)).containsExactly("시오", "니보시");
        assertThat(types(FIRST + 5)).containsExactly("마제소바", "기타");
        assertThat(types(FIRST + 6)).containsExactly("기타");
        assertThat(types(FIRST + 7)).containsExactly("기타");
    }

    private void insertShop(long id, String naverPlaceId, String ramenTypes, boolean deleted) {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, address, ramen_types, tags, naver_place_id, business_status,
                    ai_summary_keywords, view_count, log_count, bookmark_count, is_published, created_at, updated_at,
                    deleted_at)
                VALUES (?, '매장', '서울', ?, '[]', ?, 'OPERATIONAL', '[]', 0, 0, 0, TRUE,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), IF(?, CURRENT_TIMESTAMP(6), NULL))
                """, id, ramenTypes, naverPlaceId, deleted);
    }

    private List<String> types(long id) {
        String json = jdbc.queryForObject("SELECT ramen_types FROM tb_v2_shop WHERE id = ?", String.class, id);
        return mapper.readValue(json, new TypeReference<List<String>>() {
        });
    }

    private void runSplit() {
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V41__split_other_v2_shop_ramen_types.sql"))
            .execute(dataSource);
    }

}
