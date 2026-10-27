package com.raota.mobile.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.support.BaseIntegrationTest;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** 복사해 온 저장소 직접 주소만 CDN 주소로 바꾸고, 다른 주소는 그대로 둔다. */
class MobileShopImageUrlMigrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900003101L;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop_image WHERE shop_id = ?", SHOP);
    }

    @Test
    void 저장소_직접_주소를_CDN_주소로_바꾸고_다시_실행해도_같다() {
        String[] seeds = {
            "https://axrj2zzrti3z.compat.objectstorage.ap-chuncheon-1.oraclecloud.com/n/axrj2zzrti3z/b/ramenshop/o/shop_1_a.webp",
            "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/axrj2zzrti3z/b/ramenshop/o/ramen-shop/b.jpg",
            "https://images.raota.net/shops/c.webp",
            "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/axrj2zzrti3z/b/other-bucket/o/d.jpg",
            "https://res.cloudinary.com/raota/image/upload/e.jpg"
        };
        for (int order = 0; order < seeds.length; order++) {
            jdbc.update(
                    "INSERT INTO tb_v2_shop_image (shop_id, url, source, sort_order) VALUES (?, ?, 'ADMIN', ?)",
                    SHOP,
                    seeds[order],
                    order);
        }

        run();
        run();

        assertThat(jdbc.queryForList(
                        "SELECT url FROM tb_v2_shop_image WHERE shop_id = ? ORDER BY sort_order", String.class, SHOP))
                .containsExactly(
                        "https://images.raota.net/shop_1_a.webp",
                        "https://images.raota.net/ramen-shop/b.jpg",
                        "https://images.raota.net/shops/c.webp",
                        "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/axrj2zzrti3z/b/other-bucket/o/d.jpg",
                        "https://res.cloudinary.com/raota/image/upload/e.jpg");
    }

    private void run() {
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V37__normalize_v2_shop_image_urls.sql"))
                .execute(dataSource);
    }
}
