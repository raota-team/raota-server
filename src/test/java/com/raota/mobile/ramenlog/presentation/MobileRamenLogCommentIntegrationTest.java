package com.raota.mobile.ramenlog.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileRamenLogCommentIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900006501L;

    private static final long LOG = 900006511L;

    private static final long OTHER_LOG = 900006512L;

    private static final long PRIVATE_LOG = 900006513L;

    private static final Instant BASE = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    private Long author;

    private Long writer;

    private Long admin;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, is_published, created_at, updated_at)
                VALUES (?, '댓글 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        author = active();
        writer = active();
        admin = active();
        jdbc.update("UPDATE tb_v2_user SET role = 'ADMIN' WHERE id = ?", admin);
        insertLog(LOG, "PUBLIC");
        insertLog(OTHER_LOG, "PUBLIC");
        insertLog(PRIVATE_LOG, "PRIVATE");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_ramen_log_comment WHERE ramen_log_id IN (?, ?, ?)", LOG, OTHER_LOG, PRIVATE_LOG);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE shop_id = ?", SHOP);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        created.forEach(users::deleteById);
    }

    @Test
    void 같은_키로_다시_쓰면_같은_댓글을_돌려주고_다른_기록에_쓴_키는_409다() throws Exception {
        JsonNode first = data(
                create(LOG, writer, "repeat", "{\"content\":\"맛있어 보여요\"}").andExpect(status().isCreated()));
        JsonNode second = data(
                create(LOG, writer, "repeat", "{\"content\":\"맛있어 보여요\"}").andExpect(status().isCreated()));
        assertThat(second).isEqualTo(first);
        assertThat(first.get("logId").asString()).isEqualTo(String.valueOf(LOG));
        assertThat(first.get("content").asString()).isEqualTo("맛있어 보여요");
        assertThat(first.get("isMine").asBoolean()).isTrue();
        assertThat(first.get("author").get("id").asString()).isEqualTo(String.valueOf(writer));
        assertThat(commentCount(LOG)).isEqualTo(1);

        create(OTHER_LOG, writer, "repeat", "{\"content\":\"다른 기록\"}").andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        assertThat(commentCount(OTHER_LOG)).isZero();
    }

    @Test
    void 같은_키로_동시에_보내도_댓글은_하나만_생긴다() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int attempt = 0; attempt < 4; attempt++) {
            results.add(executor.submit(() -> {
                start.await();
                return create(LOG, writer, "race", "{\"content\":\"동시에\"}").andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo(201);
        }
        executor.shutdown();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log_comment WHERE ramen_log_id = ?",
                Integer.class, LOG))
            .isEqualTo(1);
        assertThat(commentCount(LOG)).isEqualTo(1);
    }

    @Test
    void 내용과_멱등키를_검증한다() throws Exception {
        for (String body : List.of("{\"content\":\"a\"}", "{\"content\":\"   \"}",
                "{\"content\":\"" + "x".repeat(501) + "\"}", "{}")) {
            create(LOG, writer, UUID.randomUUID().toString(), body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(post("/api/v2/ramen-logs/{logId}/comments", LOG).header(HttpHeaders.AUTHORIZATION, auth(writer))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"키 없음\"}")).andExpect(status().isBadRequest());
        assertThat(commentCount(LOG)).isZero();
    }

    @Test
    void 목록은_등록순으로_이어지고_활성_회원이_아닌_작성자의_댓글은_빼고_상세에_첫_20개를_붙인다() throws Exception {
        MobileUser pending = MobileUser.onboarding("comment-" + UUID.randomUUID() + "@example.com");
        pending.completeOnboarding(Nickname.of("C" + UUID.randomUUID().toString().substring(0, 8)), BASE);
        pending.requestWithdrawal(BASE, Duration.ofDays(30));
        Long withdrawing = users.saveAndFlush(pending).getId();
        created.add(withdrawing);
        List<Long> visible = new ArrayList<>();
        for (int minute = 0; minute < 24; minute++) {
            Long userId = minute % 4 == 1 ? withdrawing : writer;
            long id = insertComment(LOG, userId, minute);
            if (!userId.equals(withdrawing)) {
                visible.add(id);
            }
        }

        JsonNode first = data(mvc.perform(page(LOG).param("size", "5")).andExpect(status().isOk()));
        assertThat(ids(first)).containsExactlyElementsOf(visible.subList(0, 5));
        JsonNode second = data(
                mvc.perform(page(LOG).param("size", "20").param("cursor", first.get("nextCursor").asString()))
                    .andExpect(status().isOk()));
        assertThat(ids(second)).containsExactlyElementsOf(visible.subList(5, 18));
        assertThat(second.get("hasNext").asBoolean()).isFalse();

        JsonNode detail = data(mvc.perform(get("/api/v2/ramen-logs/{logId}", LOG)).andExpect(status().isOk()));
        List<Long> preview = new ArrayList<>();
        detail.get("commentsPreview").forEach(item -> preview.add(Long.valueOf(item.get("id").asString())));
        assertThat(preview).containsExactlyElementsOf(visible.subList(0, 18));
        assertThat(detail.get("commentsNextCursor").isNull()).isTrue();

        for (int minute = 30; minute < 33; minute++) {
            visible.add(insertComment(LOG, writer, minute));
        }
        JsonNode longer = data(mvc.perform(get("/api/v2/ramen-logs/{logId}", LOG)).andExpect(status().isOk()));
        assertThat(longer.get("commentsPreview").size()).isEqualTo(20);
        JsonNode rest = data(mvc.perform(page(LOG).param("cursor", longer.get("commentsNextCursor").asString()))
            .andExpect(status().isOk()));
        assertThat(ids(rest)).containsExactlyElementsOf(visible.subList(20, 21));
    }

    @Test
    void 작성자만_지울_수_있고_지운_댓글은_목록과_댓글_수에서_빠지며_그_키는_다시_쓸_수_없다() throws Exception {
        String id = data(create(LOG, writer, "delete-me", "{\"content\":\"지울 댓글\"}").andExpect(status().isCreated()))
            .get("id")
            .asString();
        mvc.perform(
                delete("/api/v2/ramen-log-comments/{commentId}", id).header(HttpHeaders.AUTHORIZATION, auth(author)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mvc.perform(
                delete("/api/v2/ramen-log-comments/{commentId}", id).header(HttpHeaders.AUTHORIZATION, auth(writer)))
            .andExpect(status().isNoContent());
        mvc.perform(
                delete("/api/v2/ramen-log-comments/{commentId}", id).header(HttpHeaders.AUTHORIZATION, auth(writer)))
            .andExpect(status().isNotFound());

        assertThat(commentCount(LOG)).isZero();
        assertThat(data(mvc.perform(page(LOG)).andExpect(status().isOk())).get("items").size()).isZero();
        create(LOG, writer, "delete-me", "{\"content\":\"지울 댓글\"}").andExpect(status().isConflict());
    }

    @Test
    void 같은_댓글을_동시에_지워도_한_번만_지워지고_댓글_수는_한_번만_줄어든다() throws Exception {
        create(LOG, writer, "keep", "{\"content\":\"남을 댓글\"}").andExpect(status().isCreated());
        String id = data(create(LOG, writer, "race-delete", "{\"content\":\"지울 댓글\"}").andExpect(status().isCreated()))
            .get("id")
            .asString();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int attempt = 0; attempt < 4; attempt++) {
            results.add(executor.submit(() -> {
                start.await();
                return mvc
                    .perform(delete("/api/v2/ramen-log-comments/{commentId}", id).header(HttpHeaders.AUTHORIZATION,
                            auth(writer)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        assertThat(statuses).containsExactlyInAnyOrder(204, 404, 404, 404);
        assertThat(commentCount(LOG)).isEqualTo(1);
    }

    @Test
    void 볼_수_없게_된_기록의_댓글은_누가_지워도_404이고_같은_키로_다시_보내도_404다() throws Exception {
        String id = data(create(LOG, writer, "hidden", "{\"content\":\"숨길 댓글\"}").andExpect(status().isCreated()))
            .get("id")
            .asString();
        mvc.perform(
                delete("/api/v2/ramen-log-comments/{commentId}", id).header(HttpHeaders.AUTHORIZATION, auth(author)))
            .andExpect(status().isForbidden());
        jdbc.update("UPDATE tb_v2_ramen_log SET visibility = 'PRIVATE' WHERE id = ?", LOG);

        for (Long userId : List.of(admin, writer)) {
            mvc.perform(delete("/api/v2/ramen-log-comments/{commentId}", id).header(HttpHeaders.AUTHORIZATION,
                    auth(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        }
        create(LOG, writer, "hidden", "{\"content\":\"숨길 댓글\"}").andExpect(status().isNotFound());
        assertThat(commentCount(LOG)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM tb_v2_ramen_log_comment WHERE id = ?",
                Boolean.class, Long.valueOf(id)))
            .isTrue();
    }

    @Test
    void 걸러낼_댓글이_많으면_한_요청에서_묶음_상한까지만_읽고_빈_페이지와_이어_읽을_커서를_돌려준다() throws Exception {
        MobileUser pending = MobileUser.onboarding("comment-" + UUID.randomUUID() + "@example.com");
        pending.completeOnboarding(Nickname.of("C" + UUID.randomUUID().toString().substring(0, 8)), BASE);
        pending.requestWithdrawal(BASE, Duration.ofDays(30));
        Long withdrawing = users.saveAndFlush(pending).getId();
        created.add(withdrawing);
        for (int minute = 0; minute < 12; minute++) {
            insertComment(OTHER_LOG, withdrawing, minute);
        }
        long last = insertComment(OTHER_LOG, writer, 12);

        JsonNode first = data(mvc.perform(page(OTHER_LOG).param("size", "1")).andExpect(status().isOk()));
        assertThat(ids(first)).isEmpty();
        assertThat(first.get("hasNext").asBoolean()).isTrue();
        JsonNode second = data(
                mvc.perform(page(OTHER_LOG).param("size", "1").param("cursor", first.get("nextCursor").asString()))
                    .andExpect(status().isOk()));
        assertThat(ids(second)).containsExactly(last);
        assertThat(second.get("hasNext").asBoolean()).isFalse();
    }

    @Test
    void 볼_수_없는_기록은_404이고_읽기는_누구나_쓰기는_활성_회원만_가능하다() throws Exception {
        create(PRIVATE_LOG, writer, "private", "{\"content\":\"비공개\"}").andExpect(status().isNotFound());
        mvc.perform(page(PRIVATE_LOG)).andExpect(status().isNotFound());
        mvc.perform(page(PRIVATE_LOG).header(HttpHeaders.AUTHORIZATION, auth(author))).andExpect(status().isOk());
        mvc.perform(page(900006599L)).andExpect(status().isNotFound());

        insertComment(LOG, writer, 0);
        for (MockHttpServletRequestBuilder request : List.of(page(LOG),
                page(LOG).header(HttpHeaders.AUTHORIZATION, auth(writer)),
                page(LOG).header(HttpHeaders.AUTHORIZATION, auth(admin)))) {
            mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1));
        }

        mvc.perform(post("/api/v2/ramen-logs/{logId}/comments", LOG).header("Idempotency-Key", "anonymous")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"비회원\"}")).andExpect(status().isUnauthorized());
        Long onboarding = users
            .saveAndFlush(MobileUser.onboarding("comment-onboarding-" + UUID.randomUUID() + "@example.com"))
            .getId();
        created.add(onboarding);
        create(LOG, onboarding, "onboarding", "{\"content\":\"가입 중\"}").andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
    }

    private ResultActions create(long logId, Long userId, String key, String body) throws Exception {
        return mvc
            .perform(post("/api/v2/ramen-logs/{logId}/comments", logId).header(HttpHeaders.AUTHORIZATION, auth(userId))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private MockHttpServletRequestBuilder page(long logId) {
        return get("/api/v2/ramen-logs/{logId}/comments", logId);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsByteArray()).get("data");
    }

    private List<Long> ids(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.get("items").forEach(item -> ids.add(Long.valueOf(item.get("id").asString())));
        return ids;
    }

    private int commentCount(long logId) {
        return jdbc.queryForObject("SELECT comment_count FROM tb_v2_ramen_log WHERE id = ?", Integer.class, logId);
    }

    private long insertComment(long logId, Long userId, int minute) {
        Timestamp at = Timestamp.from(BASE.plus(Duration.ofMinutes(minute)));
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tb_v2_ramen_log_comment (ramen_log_id, user_id, content, created_at, updated_at)
                    VALUES (?, ?, '댓글', ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, logId);
            statement.setLong(2, userId);
            statement.setTimestamp(3, at);
            statement.setTimestamp(4, at);
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    private void insertLog(long id, String visibility) {
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    satisfaction_score, broth_density_score, noodle_firmness_score, topping_score,
                    revisit_intention, note, taste_note_codes, visibility, created_at, updated_at)
                VALUES (?, ?, ?, '2026-09-01', '라멘', '쇼유', 4, 3, 3, 3, 'OFTEN', '', '[]', ?,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, id, author, SHOP, visibility);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("comment-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("C" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }

}
