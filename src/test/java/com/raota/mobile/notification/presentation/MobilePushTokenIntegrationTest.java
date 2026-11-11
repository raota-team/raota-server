package com.raota.mobile.notification.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class MobilePushTokenIntegrationTest extends BaseIntegrationTest {

    private static final String TOKEN = "ExponentPushToken[abc-123_XYZ]";

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

    private MockMvc mvc;

    private final List<Long> members = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void cleanUp() {
        for (Long member : members) {
            jdbc.update("DELETE FROM tb_v2_push_token WHERE user_id = ?", member);
            users.deleteById(member);
        }
    }

    @Test
    void 같은_토큰을_다시_보내면_한_행을_갱신하고_다른_회원이_보내면_그_회원에게_옮긴다() throws Exception {
        Long owner = active();
        Long next = active();

        register(owner, TOKEN, "ANDROID", null).andExpect(status().isNoContent());
        assertThat(row(TOKEN)).containsEntry("user_id", owner).containsEntry("language", "KO");

        register(owner, TOKEN, "ANDROID", "es-ES,es;q=0.9").andExpect(status().isNoContent());
        assertThat(row(TOKEN)).containsEntry("language", "ES");

        register(next, TOKEN, "IOS", "zh-CN").andExpect(status().isNoContent());
        assertThat(count(TOKEN)).isEqualTo(1);
        assertThat(row(TOKEN))
                .containsEntry("user_id", next)
                .containsEntry("platform", "IOS")
                .containsEntry("language", "ZH");
    }

    @Test
    void 해제는_내_토큰만_지우고_없는_토큰도_성공으로_답한다() throws Exception {
        Long owner = active();
        Long stranger = active();
        register(owner, TOKEN, "IOS", "ko-KR").andExpect(status().isNoContent());

        unregister(stranger, TOKEN).andExpect(status().isNoContent());
        assertThat(count(TOKEN)).isEqualTo(1);

        unregister(owner, TOKEN).andExpect(status().isNoContent());
        assertThat(count(TOKEN)).isZero();
        unregister(owner, TOKEN).andExpect(status().isNoContent());
    }

    @Test
    void 온보딩_중인_회원은_등록할_수_없지만_로그아웃_때의_해제는_할_수_있다() throws Exception {
        MobileUser user = MobileUser.onboarding("push-" + UUID.randomUUID() + "@example.com");
        Long onboarding = users.saveAndFlush(user).getId();
        members.add(onboarding);

        register(onboarding, TOKEN, "IOS", "ko-KR").andExpect(status().isForbidden());
        unregister(onboarding, TOKEN).andExpect(status().isNoContent());
        mvc.perform(post("/api/v2/members/me/push-tokens")).andExpect(status().isUnauthorized());
    }

    @Test
    void Expo_토큰_형식이_아니거나_플랫폼이_없으면_거절한다() throws Exception {
        Long owner = active();

        register(owner, "not-a-token", "IOS", null).andExpect(status().isBadRequest());
        register(owner, "ExponentPushToken[]", "IOS", null).andExpect(status().isBadRequest());
        mvc.perform(authorized(post("/api/v2/members/me/push-tokens"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(TOKEN)))
                .andExpect(status().isBadRequest());
    }

    private ResultActions register(Long member, String token, String platform, String acceptLanguage) throws Exception {
        MockHttpServletRequestBuilder request = authorized(post("/api/v2/members/me/push-tokens"), member)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"platform\":\"%s\"}".formatted(token, platform));
        if (acceptLanguage != null) {
            request.header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
        }
        return mvc.perform(request);
    }

    /** 토큰의 대괄호는 경로에서 %5B·%5D로 인코딩되어 간다. */
    private ResultActions unregister(Long member, String token) throws Exception {
        return mvc.perform(authorized(delete("/api/v2/members/me/push-tokens/{token}", token), member));
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, Long member) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(member));
    }

    private Map<String, Object> row(String token) {
        return jdbc.queryForMap("SELECT user_id, platform, language FROM tb_v2_push_token WHERE token = ?", token);
    }

    private int count(String token) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_push_token WHERE token = ?", Integer.class, token);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("push-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("P" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        members.add(id);
        return id;
    }
}
