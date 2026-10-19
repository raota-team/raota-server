package com.raota.mobile.common.copy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * v1 → v2 복사(#59) 세 단계를 v1 fixture로 실행해 규칙 경계, 대조표 등식, 재실행 멱등성, 관리자 권한을 확인한다. 복사는 테이블 전체를
 * 읽으므로 각 테스트는 한 트랜잭션 안에서 v1·v2 원본 테이블을 비우고 시작해 끝나면 되돌린다.
 */
@Transactional
class LegacyCopyIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/admin/api/v2/copy";

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    private static final long SHOP_A = 900008001L;

    private static final long SHOP_B = 900008002L;

    private static final long SHOP_MISSING = 900008009L;

    private static final String V13_TIME = "2026-06-12 01:21:21.695633";

    private static final List<String> TABLES = List.of("tb_member_profile", "tb_social_account", "tb_ramen_log",
            "tb_ramen_log_like", "tb_bookmark", "tb_v2_user", "tb_v2_user_oauth_account", "tb_v2_ramen_log",
            "tb_v2_ramen_log_image", "tb_v2_ramen_log_like", "tb_v2_shop_bookmark");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MobileUserRepository users;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (String table : TABLES) {
            jdbc.update("DELETE FROM " + table);
        }
        for (long shopId : List.of(SHOP_A, SHOP_B)) {
            jdbc.update("""
                    INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                        ai_summary_keywords, log_count, bookmark_count, is_published, created_at, updated_at)
                    VALUES (?, '복사 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', 5, 7, TRUE,
                        '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                    """, shopId);
        }
        v2User(2001L, "가져간닉", "2026-09-01 00:00:00");
        v2User(1008L, "다른사람", "2026-09-02 00:00:00");

        member(1001L, "RamenKing", "king@example.com", "", "안녕하세요", true, "2026-07-01 12:00:00", null);
        member(1002L, "탈퇴회원", "gone@example.com", null, null, true, "2026-07-01 12:00:00", "2026-08-01 00:00:00");
        member(1003L, "라멘 왕!", null, "https://img.example/a.png", null, true, "2026-07-01 12:00:00", null);
        member(1004L, "ramenking", null, null, null, true, "2026-07-01 12:00:00", null);
        member(1005L, "면러버", "   ", null, "", true, V13_TIME, null);
        member(1006L, "온보딩중", null, null, null, false, "2026-07-01 12:00:00", null);
        member(1007L, "가져간닉", null, null, null, true, "2026-07-01 12:00:00", null);
        member(1008L, "아이디충돌", null, null, null, true, "2026-07-01 12:00:00", null);

        social(1001L, "KAKAO", "k-1", "king@example.com");
        social(9999L, "KAKAO", "k-orphan", null);
        social(1002L, "GOOGLE", "g-2", null);
        social(1001L, "KAKAO", "k-2", null);
        social(1008L, "GOOGLE", "g-8", null);
        social(1005L, "GOOGLE", "g-5", "");

        log(3001L, 1001L, SHOP_A, "https://res.cloudinary.com/raota/1.jpg", null, "[\"진해요\",\"없는 라벨\"]",
                "[\"국물 흡착 좋아요\",\"국물이 잘 배어요\"]", null, "[\"차슈 좋아요\"]", "DEFINITELY", true, "쇼유", false);
        log(3002L, 1001L, SHOP_B, "", "두 번째", null, null, null, null, "ONCE_ENOUGH", false, "우동", false);
        log(3003L, 1001L, SHOP_A, "https://res.cloudinary.com/raota/3.jpg", null, null, null, null, null, "SOMETIMES",
                true, "쇼유", true);
        log(3004L, 1002L, SHOP_A, "https://res.cloudinary.com/raota/4.jpg", null, null, null, null, null, "SOMETIMES",
                true, "쇼유", false);
        log(3005L, 1001L, SHOP_MISSING, "https://res.cloudinary.com/raota/5.jpg", null, null, null, null, null,
                "SOMETIMES", true, "쇼유", false);
        log(3006L, 1001L, SHOP_A, "https://res.cloudinary.com/raota/6.jpg", null, null, null, null, null, "MAYBE", true,
                "쇼유", false);
        log(3007L, 1003L, SHOP_A, "https://res.cloudinary.com/raota/7.jpg", "맛있음", null, null, "[\"딱 좋아요\"]", null,
                "SOMETIMES", true, "돈코츠", false);

        like(3001L, 1003L);
        like(3001L, 1005L);
        like(3003L, 1001L);
        like(3001L, 1002L);
        like(3007L, 1001L);

        bookmark(1001L, SHOP_A, "2026-07-03 10:00:00", false);
        bookmark(1001L, SHOP_A, "2026-07-04 10:00:00", false);
        bookmark(1003L, SHOP_A, "2026-07-03 10:00:00", true);
        bookmark(1002L, SHOP_B, "2026-07-03 10:00:00", false);
        bookmark(1005L, SHOP_MISSING, "2026-07-03 10:00:00", false);
        bookmark(1005L, SHOP_B, "2026-07-05 10:00:00", false);
    }

    @Test
    void 회원_기록_북마크를_규칙대로_복사하고_대조표_등식이_성립한다() throws Exception {
        JsonNode members = copy("members");
        assertTable(members.get(0), "tb_v2_user", 8, 6, 0, Map.of("WITHDRAWN_MEMBER", 1, "ID_CONFLICT", 1));
        assertTable(members.get(1), "tb_v2_user_oauth_account", 6, 2, 0,
                Map.of("ORPHAN", 1, "WITHDRAWN_MEMBER", 1, "DUPLICATE_PROVIDER", 1, "MEMBER_NOT_COPIED", 1));

        Map<String, Object> king = row("SELECT * FROM tb_v2_user WHERE id = 1001");
        assertThat(king).containsEntry("nickname", "RamenKing")
            .containsEntry("nickname_normalized", "ramenking")
            .containsEntry("email", "king@example.com")
            .containsEntry("avatar_url", null)
            .containsEntry("bio", "안녕하세요")
            .containsEntry("role", "USER")
            .containsEntry("status", "ACTIVE")
            .containsEntry("log_count", 0);
        assertThat(time("SELECT created_at FROM tb_v2_user WHERE id = 1001")).isEqualTo("2026-07-01T03:00");
        assertThat(time("SELECT onboarding_completed_at FROM tb_v2_user WHERE id = 1001"))
            .isEqualTo("2026-07-01T03:00");
        MobileUser loaded = users.findById(1001L).orElseThrow();
        assertThat(loaded.getCreatedAt()).isEqualTo(Instant.parse("2026-07-01T03:00:00Z"));

        assertOnboardingWithoutNickname(1003L);
        assertThat(text("SELECT avatar_url FROM tb_v2_user WHERE id = 1003")).isEqualTo("https://img.example/a.png");
        assertOnboardingWithoutNickname(1004L);
        assertOnboardingWithoutNickname(1007L);
        assertThat(row("SELECT * FROM tb_v2_user WHERE id = 1005")).containsEntry("status", "ACTIVE")
            .containsEntry("email", null)
            .containsEntry("bio", null);
        assertThat(time("SELECT created_at FROM tb_v2_user WHERE id = 1005")).isEqualTo("2026-06-12T01:21:21.695633");
        assertThat(row("SELECT * FROM tb_v2_user WHERE id = 1006")).containsEntry("status", "ONBOARDING")
            .containsEntry("nickname", "온보딩중")
            .containsEntry("onboarding_completed_at", null);
        assertThat(count("SELECT COUNT(*) FROM tb_v2_user WHERE id = 1002")).isZero();
        assertThat(text("SELECT nickname FROM tb_v2_user WHERE id = 1008")).isEqualTo("다른사람");
        assertThat(count("SELECT COUNT(*) FROM tb_v2_user_consent WHERE user_id BETWEEN 1001 AND 1007")).isZero();

        assertThat(text("""
                SELECT CONCAT(provider, ':', provider_subject, ':', IFNULL(provider_email, '-'), ':', linked_at)
                FROM tb_v2_user_oauth_account WHERE user_id = 1005
                """)).isEqualTo("GOOGLE:g-5:-:" + V13_TIME);
        assertThat(text("SELECT provider_subject FROM tb_v2_user_oauth_account WHERE user_id = 1001")).isEqualTo("k-1");
        assertThat(time("SELECT last_login_at FROM tb_v2_user_oauth_account WHERE user_id = 1001"))
            .isEqualTo("2026-07-01T03:00");

        JsonNode logs = copy("ramen-logs");
        Map<String, Integer> logExclusions = Map.of("DELETED_LOG", 1, "MEMBER_NOT_COPIED", 1, "SHOP_NOT_COPIED", 1,
                "UNKNOWN_REVISIT", 1);
        assertTable(logs.get(0), "tb_v2_ramen_log", 7, 3, 0, logExclusions);
        Map<String, Integer> imageExclusions = new HashMap<>(logExclusions);
        imageExclusions.put("NO_IMAGE", 1);
        assertTable(logs.get(1), "tb_v2_ramen_log_image", 7, 2, 0, imageExclusions);
        assertTable(logs.get(2), "tb_v2_ramen_log_like", 5, 3, 0, Map.of("LOG_NOT_COPIED", 1, "MEMBER_NOT_COPIED", 1));

        assertThat(row("SELECT * FROM tb_v2_ramen_log WHERE id = 3001")).containsEntry("user_id", 1001L)
            .containsEntry("shop_id", SHOP_A)
            .containsEntry("menu_name", "특제 메뉴")
            .containsEntry("ramen_type", "쇼유")
            .containsEntry("revisit_intention", "OFTEN")
            .containsEntry("visibility", "PUBLIC")
            .containsEntry("note", "")
            .containsEntry("satisfaction_score", null)
            .containsEntry("topping_score", null)
            .containsEntry("like_count", 2)
            .containsEntry("comment_count", 0)
            .containsEntry("idempotency_key", "v1-ramen-log-3001");
        assertThat(mapper.readTree(text("SELECT taste_note_codes FROM tb_v2_ramen_log WHERE id = 3001")))
            .isEqualTo(mapper.readTree("[\"BROTH_01\",\"NOODLE_04\",\"TOPPING_01\"]"));
        assertThat(text("SELECT CAST(visited_at AS CHAR) FROM tb_v2_ramen_log WHERE id = 3001"))
            .isEqualTo("2026-06-30");
        assertThat(time("SELECT created_at FROM tb_v2_ramen_log WHERE id = 3001")).isEqualTo("2026-07-02T00:30");
        assertThat(text("SELECT url FROM tb_v2_ramen_log_image WHERE ramen_log_id = 3001 AND sort_order = 0"))
            .isEqualTo("https://res.cloudinary.com/raota/1.jpg");
        assertThat(row("SELECT * FROM tb_v2_ramen_log WHERE id = 3002")).containsEntry("ramen_type", "기타")
            .containsEntry("revisit_intention", "ONCE_IS_ENOUGH")
            .containsEntry("visibility", "PRIVATE")
            .containsEntry("note", "두 번째")
            .containsEntry("like_count", 0);
        assertThat(count("SELECT COUNT(*) FROM tb_v2_ramen_log_image WHERE ramen_log_id = 3002")).isZero();
        assertThat(row("SELECT * FROM tb_v2_ramen_log WHERE id = 3007")).containsEntry("revisit_intention", "SOMETIMES")
            .containsEntry("like_count", 1);
        assertThat(count("SELECT COUNT(*) FROM tb_v2_ramen_log WHERE id IN (3003, 3004, 3005, 3006)")).isZero();
        assertThat(count("SELECT log_count FROM tb_v2_user WHERE id = 1001")).isEqualTo(2);
        assertThat(count("SELECT log_count FROM tb_v2_user WHERE id = 1003")).isEqualTo(1);
        assertThat(count("SELECT log_count FROM tb_v2_shop WHERE id = " + SHOP_A)).isEqualTo(2);
        assertThat(count("SELECT log_count FROM tb_v2_shop WHERE id = " + SHOP_B)).isEqualTo(1);
        assertThat(time("SELECT created_at FROM tb_v2_ramen_log_like WHERE user_id = 1003 AND ramen_log_id = 3001"))
            .isEqualTo("2026-07-02T01:00");

        JsonNode bookmarks = copy("bookmarks");
        assertTable(bookmarks.get(0), "tb_v2_shop_bookmark", 6, 2, 0,
                Map.of("DUPLICATE_BOOKMARK", 1, "CANCELED", 1, "MEMBER_NOT_COPIED", 1, "SHOP_NOT_COPIED", 1));
        assertThat(time("SELECT created_at FROM tb_v2_shop_bookmark WHERE user_id = 1001 AND shop_id = " + SHOP_A))
            .isEqualTo("2026-07-03T01:00");
        assertThat(count("SELECT bookmark_count FROM tb_v2_shop WHERE id = " + SHOP_A)).isEqualTo(1);
        assertThat(count("SELECT bookmark_count FROM tb_v2_shop WHERE id = " + SHOP_B)).isEqualTo(1);
    }

    @Test
    void 다시_실행하면_모두_이미_복사됨으로_세고_행을_더하지_않는다() throws Exception {
        copy("members");
        copy("ramen-logs");
        copy("bookmarks");
        Map<String, Integer> before = rowCounts();

        JsonNode members = copy("members");
        assertTable(members.get(0), "tb_v2_user", 8, 0, 6, Map.of("WITHDRAWN_MEMBER", 1, "ID_CONFLICT", 1));
        assertTable(members.get(1), "tb_v2_user_oauth_account", 6, 0, 2,
                Map.of("ORPHAN", 1, "WITHDRAWN_MEMBER", 1, "DUPLICATE_PROVIDER", 1, "MEMBER_NOT_COPIED", 1));
        JsonNode logs = copy("ramen-logs");
        Map<String, Integer> logExclusions = Map.of("DELETED_LOG", 1, "MEMBER_NOT_COPIED", 1, "SHOP_NOT_COPIED", 1,
                "UNKNOWN_REVISIT", 1);
        assertTable(logs.get(0), "tb_v2_ramen_log", 7, 0, 3, logExclusions);
        Map<String, Integer> imageExclusions = new HashMap<>(logExclusions);
        imageExclusions.put("NO_IMAGE", 1);
        assertTable(logs.get(1), "tb_v2_ramen_log_image", 7, 0, 2, imageExclusions);
        assertTable(logs.get(2), "tb_v2_ramen_log_like", 5, 0, 3, Map.of("LOG_NOT_COPIED", 1, "MEMBER_NOT_COPIED", 1));
        JsonNode bookmarks = copy("bookmarks");
        assertTable(bookmarks.get(0), "tb_v2_shop_bookmark", 6, 0, 2,
                Map.of("DUPLICATE_BOOKMARK", 1, "CANCELED", 1, "MEMBER_NOT_COPIED", 1, "SHOP_NOT_COPIED", 1));

        assertThat(rowCounts()).isEqualTo(before);
        assertThat(count("SELECT like_count FROM tb_v2_ramen_log WHERE id = 3001")).isEqualTo(2);
        assertThat(count("SELECT log_count FROM tb_v2_user WHERE id = 1001")).isEqualTo(2);
        assertThat(count("SELECT bookmark_count FROM tb_v2_shop WHERE id = " + SHOP_A)).isEqualTo(1);
    }

    @Test
    void 회원을_복사하기_전에는_기록과_북마크를_회원_미복사로_제외한다() throws Exception {
        JsonNode logs = copy("ramen-logs");
        assertTable(logs.get(0), "tb_v2_ramen_log", 7, 0, 0, Map.of("DELETED_LOG", 1, "MEMBER_NOT_COPIED", 6));
        JsonNode bookmarks = copy("bookmarks");
        assertTable(bookmarks.get(0), "tb_v2_shop_bookmark", 6, 0, 0,
                Map.of("DUPLICATE_BOOKMARK", 1, "CANCELED", 1, "MEMBER_NOT_COPIED", 4));
        assertThat(count("SELECT COUNT(*) FROM tb_v2_ramen_log")).isZero();
        assertThat(count("SELECT COUNT(*) FROM tb_v2_shop_bookmark")).isZero();
    }

    @Test
    void 웹_관리자만_실행할_수_있다() throws Exception {
        for (String step : List.of("members", "ramen-logs", "bookmarks")) {
            mvc.perform(post(BASE + "/" + step)).andExpect(status().isUnauthorized());
            mvc.perform(post(BASE + "/" + step).with(user("member").roles("USER"))).andExpect(status().isForbidden());
        }
        assertThat(count("SELECT COUNT(*) FROM tb_v2_user")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM tb_v2_ramen_log")).isZero();
    }

    private JsonNode copy(String step) throws Exception {
        byte[] body = mvc.perform(post(BASE + "/" + step).with(ADMIN))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        return mapper.readTree(body).get("data").get("tables");
    }

    private void assertTable(JsonNode table, String name, int source, int copied, int alreadyCopied,
            Map<String, Integer> excluded) {
        Map<String, Integer> actualExcluded = new HashMap<>();
        int excludedTotal = 0;
        for (Map.Entry<String, JsonNode> entry : table.get("excluded").properties()) {
            actualExcluded.put(entry.getKey(), entry.getValue().asInt());
            excludedTotal += entry.getValue().asInt();
        }
        assertThat(table.get("table").asString()).isEqualTo(name);
        assertThat(actualExcluded).as(name).isEqualTo(excluded);
        assertThat(table.get("copied").asInt()).as(name).isEqualTo(copied);
        assertThat(table.get("alreadyCopied").asInt()).as(name).isEqualTo(alreadyCopied);
        assertThat(table.get("source").asInt()).as(name).isEqualTo(source);
        assertThat(copied + alreadyCopied + excludedTotal).as(name).isEqualTo(source);
    }

    private void assertOnboardingWithoutNickname(long id) {
        assertThat(row("SELECT * FROM tb_v2_user WHERE id = " + id)).containsEntry("status", "ONBOARDING")
            .containsEntry("nickname", null)
            .containsEntry("nickname_normalized", null)
            .containsEntry("onboarding_completed_at", null);
    }

    private Map<String, Integer> rowCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String table : TABLES) {
            counts.put(table, count("SELECT COUNT(*) FROM " + table));
        }
        return counts;
    }

    private Map<String, Object> row(String sql) {
        return jdbc.queryForMap(sql);
    }

    private String text(String sql) {
        return jdbc.queryForObject(sql, String.class);
    }

    private String time(String sql) {
        return jdbc.queryForObject(sql, LocalDateTime.class).toString();
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private void v2User(long id, String nickname, String createdAt) {
        jdbc.update("""
                INSERT INTO tb_v2_user (id, nickname, nickname_normalized, role, status, onboarding_completed_at,
                    created_at, updated_at)
                VALUES (?, ?, ?, 'USER', 'ACTIVE', ?, ?, ?)
                """, id, nickname, nickname, createdAt, createdAt, createdAt);
    }

    private void member(long id, String nickname, String email, String imageUrl, String bio, boolean registered,
            String createdAt, String deletedAt) {
        jdbc.update("""
                INSERT INTO tb_member_profile (id, nickname, email, image_url, bio, is_registration_completed,
                    created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, nickname, email, imageUrl, bio, registered, createdAt, createdAt, deletedAt);
    }

    private void social(long memberId, String provider, String providerId, String email) {
        jdbc.update("""
                INSERT INTO tb_social_account (member_id, provider, provider_id, email, nickname)
                VALUES (?, ?, ?, ?, '소셜')
                """, memberId, provider, providerId, email);
    }

    private void log(long id, long memberId, long shopId, String imageUrl, String note, String broth, String noodle,
            String seasoning, String topping, String revisit, boolean isPublic, String ramenType, boolean deleted) {
        jdbc.update("""
                INSERT INTO tb_ramen_log (id, ramen_shop_id, member_id, image_url, note, broth_notes, noodle_notes,
                    seasoning_notes, topping_notes, revisit, is_public, visited_at, created_at, updated_at, menu_name,
                    ramen_type, is_deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '2026-06-30', '2026-07-02 09:30:00', '2026-07-02 09:30:00',
                    '특제 메뉴', ?, ?)
                """, id, shopId, memberId, imageUrl, note, broth, noodle, seasoning, topping, revisit, isPublic,
                ramenType, deleted);
    }

    private void like(long logId, long memberId) {
        jdbc.update("""
                INSERT INTO tb_ramen_log_like (ramen_log_id, member_id, created_at)
                VALUES (?, ?, '2026-07-02 10:00:00')
                """, logId, memberId);
    }

    private void bookmark(long memberId, long shopId, String markingAt, boolean deleted) {
        jdbc.update("""
                INSERT INTO tb_bookmark (member_profile_id, ramen_shop_id, marking_at, is_deleted)
                VALUES (?, ?, ?, ?)
                """, memberId, shopId, markingAt, deleted);
    }

}
