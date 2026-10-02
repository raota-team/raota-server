package com.raota.mobile.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.support.BaseIntegrationTest;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** 라멘 종류 보강은 비어 있는 매장만 채우고 이에케는 기존 종류 뒤에 덧붙여야 한다. */
class MobileShopRamenTypeBackfillMigrationTest extends BaseIntegrationTest {

    private static final long FIRST = 900003001L;

    private static final long LAST = 900003008L;

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
    void 이에케는_덧붙이고_빈_매장만_태그로_채우며_다시_실행해도_같다() {
        Map<Long, String[]> seeds = Map.of(FIRST, new String[] { "[]", "[\"이에케라멘\", \"일식\"]" }, FIRST + 1,
                new String[] { "[\"돈코츠\"]", "[\"돈코츠라멘\", \"홍대이에케라멘\"]" }, FIRST + 2,
                new String[] { "[\"이에케\"]", "[\"이에케\"]" }, FIRST + 3, new String[] { "[]", "[\"탄탄면\", \"혼밥\"]" },
                FIRST + 4, new String[] { "[]", "[\"지로계\"]" }, FIRST + 5, new String[] { "[]", "[\"홍대라멘\", \"점심\"]" },
                FIRST + 6, new String[] { "[]", "[]" }, FIRST + 7, new String[] { "[\"쇼유\"]", "[\"니보시라멘\"]" });
        seeds.forEach((id, values) -> jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, address, ramen_types, tags, business_status, ai_summary_keywords,
                    view_count, log_count, bookmark_count, is_published, created_at, updated_at)
                VALUES (?, '매장', '서울', ?, ?, 'OPERATIONAL', '[]', 0, 0, 0, TRUE,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, values[0], values[1]));

        runBackfill();
        assertTypes();
        runBackfill();
        assertTypes();
    }

    private void assertTypes() {
        assertThat(types(FIRST)).containsExactly("이에케");
        assertThat(types(FIRST + 1)).containsExactly("돈코츠", "이에케");
        assertThat(types(FIRST + 2)).containsExactly("이에케");
        assertThat(types(FIRST + 3)).containsExactly("탄탄멘");
        assertThat(types(FIRST + 4)).containsExactly("기타");
        assertThat(types(FIRST + 5)).isEmpty();
        assertThat(types(FIRST + 6)).isEmpty();
        assertThat(types(FIRST + 7)).containsExactly("쇼유");
    }

    private List<String> types(long id) {
        String json = jdbc.queryForObject("SELECT ramen_types FROM tb_v2_shop WHERE id = ?", String.class, id);
        return mapper.readValue(json, new TypeReference<List<String>>() {
        });
    }

    private void runBackfill() {
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V30__backfill_v2_shop_ramen_types.sql"))
            .execute(dataSource);
    }

}
