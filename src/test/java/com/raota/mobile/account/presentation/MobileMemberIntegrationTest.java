package com.raota.mobile.account.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class MobileMemberIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService accessTokens;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Set<Long> createdUserIds = new HashSet<>();

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
    }

    @AfterEach
    void cleanUp() {
        for (Long userId : createdUserIds) {
            jdbcTemplate.update("DELETE FROM tb_v2_user_consent WHERE user_id = ?", userId);
            users.deleteById(userId);
        }
        createdUserIds.clear();
    }

    @Test
    void 온보딩_회원은_닉네임이_없는_자신의_프로필을_조회한다() throws Exception {
        Long userId = createUser();

        mvc.perform(get("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.status").value("ONBOARDING"))
            .andExpect(jsonPath("$.data.nickname").value(nullValue()))
            .andExpect(jsonPath("$.data.email").value("member@example.com"));
    }

    @Test
    void 프로필의_네_필드만_수정하고_빈_문자열은_비우며_누락한_필드는_유지한다() throws Exception {
        Long userId = createUser();

        mvc.perform(patch("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                    """
                            {"email":"updated@example.com","avatarUrl":"https://example.com/a.png","bio":"좋은 라멘","favoriteRamenType":"돈코츠","status":"ACTIVE","nickname":"몰래변경"}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("updated@example.com"))
            .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/a.png"))
            .andExpect(jsonPath("$.data.bio").value("좋은 라멘"))
            .andExpect(jsonPath("$.data.favoriteRamenType").value("돈코츠"))
            .andExpect(jsonPath("$.data.status").value("ONBOARDING"))
            .andExpect(jsonPath("$.data.nickname").value(nullValue()));

        mvc.perform(patch("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"email":"","avatarUrl":"","bio":""}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(nullValue()))
            .andExpect(jsonPath("$.data.avatarUrl").value(nullValue()))
            .andExpect(jsonPath("$.data.bio").value(nullValue()))
            .andExpect(jsonPath("$.data.favoriteRamenType").value("돈코츠"));

        assertThat(users.findById(userId).orElseThrow().getStatus().name()).isEqualTo("ONBOARDING");
    }

    @Test
    void 잘못된_프로필_필드는_필드_오류와_함께_400이다() throws Exception {
        Long userId = createUser();

        for (String fieldAndValue : new String[] { "\"email\":\"invalid\"",
                "\"avatarUrl\":\"http://example.com/a.png\"", "\"bio\":\"" + "a".repeat(501) + "\"" }) {
            mvc.perform(patch("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + fieldAndValue + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
        }
    }

    @Test
    void 사용할_수_없는_회원은_프로필_조회와_수정을_거부한다() throws Exception {
        Long userId = createUser();
        jdbcTemplate.update("UPDATE tb_v2_user SET status = 'SUSPENDED' WHERE id = ?", userId);

        mvc.perform(get("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.message").value("사용할 수 없는 계정입니다."));
        mvc.perform(patch("/api/v2/members/me").header(HttpHeaders.AUTHORIZATION, authorization(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 인증하지_않은_프로필_요청은_v2_401_응답이다() throws Exception {
        mvc.perform(get("/api/v2/members/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(patch("/api/v2/members/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 닉네임_중복_확인은_표시값을_돌려주며_내_닉네임은_사용_가능하다() throws Exception {
        Long userId = createUser();
        Long otherId = createUser();
        jdbcTemplate.update("UPDATE tb_v2_user SET nickname = 'Ramen', nickname_normalized = 'ramen' WHERE id = ?",
                otherId);

        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "  Ｎｅｗ_９  ")
            .header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nickname").value("New_9"))
            .andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "rAmEn")
            .header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(false));
        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "ＲＡＭＥＮ")
            .header(HttpHeaders.AUTHORIZATION, authorization(otherId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void 유효하지_않은_닉네임은_400이고_익명_중복_조회는_401이다() throws Exception {
        Long userId = createUser();

        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "라멘🍜")
            .header(HttpHeaders.AUTHORIZATION, authorization(userId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.message").value("닉네임은 2~12자의 한글·영문·숫자·밑줄만 쓸 수 있습니다."));
        mvc.perform(get("/api/v2/members/nickname-availability").param("nickname", "라멘"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private Long createUser() {
        Long userId = users.saveAndFlush(MobileUser.onboarding("member@example.com")).getId();
        createdUserIds.add(userId);
        return userId;
    }

    private String authorization(Long userId) {
        return "Bearer " + accessTokens.issue(userId);
    }

}
