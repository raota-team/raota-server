package com.raota.mobile.ramenlog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.global.file.cleanup.StoredImageReferences;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MobileRamenLogStoredImagesIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900005101L;

    private static final long LIVE_LOG = 900005102L;

    private static final long DELETED_LOG = 900005103L;

    private static final String BASE = "https://images.raota.net/v2/ramen-logs/";

    @Autowired
    private MobileRamenLogStoredImages references;

    @Autowired
    private List<StoredImageReferences> contributors;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private Long userId;

    @BeforeEach
    void setUp() {
        MobileUser user = MobileUser.onboarding("stored-images-log@example.com");
        user.completeOnboarding(Nickname.of("StoredLog"), Instant.parse("2026-09-01T00:00:00Z"));
        userId = users.saveAndFlush(user).getId();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, address, ramen_types, tags, business_status, ai_summary_keywords,
                    log_count, satisfaction_score_sum, scored_log_count, is_published, created_at, updated_at)
                VALUES (?, '사진 매장', '서울', '[]', '[]', 'OPERATIONAL', '[]', 0, 0, 0, TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        insertLog(LIVE_LOG, null);
        insertLog(DELETED_LOG, "2026-09-02 00:00:00");
        jdbc.update("INSERT INTO tb_v2_ramen_log_image (ramen_log_id, url, sort_order) VALUES (?, ?, 0), (?, ?, 1)",
                LIVE_LOG, BASE + "live.jpg", LIVE_LOG, BASE + "live-2.jpg");
        jdbc.update("INSERT INTO tb_v2_ramen_log_image (ramen_log_id, url, sort_order) VALUES (?, ?, 0)", DELETED_LOG,
                BASE + "deleted.jpg");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_ramen_log_image WHERE ramen_log_id IN (?, ?)", LIVE_LOG, DELETED_LOG);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE id IN (?, ?)", LIVE_LOG, DELETED_LOG);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        users.deleteById(userId);
    }

    @Test
    void 기록_사진_접두어를_맡고_지운_기록의_사진도_참조로_본다() {
        assertThat(contributors).contains(references);
        assertThat(references.prefix()).isEqualTo("v2/ramen-logs/");

        assertThat(references
            .referencedUrls(List.of(BASE + "live.jpg", BASE + "deleted.jpg", BASE + "orphan.jpg", BASE + "live.jpg")))
            .containsExactlyInAnyOrder(BASE + "live.jpg", BASE + "deleted.jpg");
    }

    @Test
    void 주소가_많으면_나눠서_조회해도_참조를_모두_찾는다() {
        List<String> urls = new ArrayList<>();
        for (int index = 0; index < 1200; index++) {
            urls.add(BASE + "orphan-" + index + ".jpg");
        }
        urls.add(BASE + "live-2.jpg");
        urls.add(BASE + "deleted.jpg");

        assertThat(references.referencedUrls(urls)).containsExactlyInAnyOrder(BASE + "live-2.jpg",
                BASE + "deleted.jpg");
    }

    private void insertLog(long id, String deletedAt) {
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    revisit_intention, note, taste_note_codes, visibility, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, '2026-09-01', '라멘', '쇼유', 'OFTEN', '', '[]', 'PUBLIC',
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00', ?)
                """, id, userId, SHOP, deletedAt);
    }

}
