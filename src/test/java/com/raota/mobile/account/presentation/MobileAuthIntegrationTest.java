package com.raota.mobile.account.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.application.port.RefreshTokenStore;
import com.raota.mobile.account.application.result.SocialIdentity;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.account.domain.repository.MobileUserOAuthAccountRepository;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.external.GoogleIdTokenVerifier;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.mobile.account.infrastructure.auth.MobileAuthProperties;
import com.raota.mobile.account.infrastructure.external.KakaoAccessTokenVerifier;
import com.raota.support.BaseIntegrationTest;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileAuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MobileUserOAuthAccountRepository accounts;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private RefreshTokenStore refreshTokens;

    @Autowired
    private MobileAuthProperties mobileAuthProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KakaoAccessTokenVerifier kakaoVerifier;

    @MockitoBean
    private GoogleIdTokenVerifier googleVerifier;

    private final Set<Long> createdUserIds = ConcurrentHashMap.newKeySet();

    private final Map<String, Long> issuedRefreshTokens = new ConcurrentHashMap<>();

    private MockMvc mvc;

    private String subject;

    private String email;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        subject = UUID.randomUUID().toString();
        email = "same@example.com";
        when(kakaoVerifier.provider()).thenReturn(OAuthProvider.KAKAO);
        when(googleVerifier.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(kakaoVerifier.verify(any(SocialCredential.class)))
            .thenAnswer(invocation -> new SocialIdentity(OAuthProvider.KAKAO, subject, email));
        when(googleVerifier.verify(any(SocialCredential.class)))
            .thenAnswer(invocation -> new SocialIdentity(OAuthProvider.GOOGLE, subject, email));
    }

    @AfterEach
    void cleanUp() {
        issuedRefreshTokens.forEach((token, userId) -> refreshTokens.revoke(token, userId));
        for (OAuthProvider provider : Set.of(OAuthProvider.KAKAO, OAuthProvider.GOOGLE)) {
            accounts.findByProviderAndProviderSubject(provider, subject).ifPresent(accounts::delete);
        }
        createdUserIds.forEach(users::deleteById);
    }

    @Test
    void 첫_로그인과_재로그인은_같은_계정이며_액세스_토큰으로_인증할_수_있다() throws Exception {
        JsonNode first = login("KAKAO", "accessToken");
        assertThat(first.get("isNewMember").asBoolean()).isTrue();
        assertThat(first.get("member").get("status").asString()).isEqualTo("ONBOARDING");
        assertThat(first.get("member").get("id").isString()).isTrue();

        mvc.perform(get("/api/v2/unknown").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + first.get("accessToken").asString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        JsonNode second = login("KAKAO", "accessToken");
        assertThat(second.get("isNewMember").asBoolean()).isFalse();
        assertThat(second.get("member").get("id").asString()).isEqualTo(first.get("member").get("id").asString());
    }

    @Test
    void 같은_이메일이라도_제공자가_다르면_별도의_회원을_만든다() throws Exception {
        JsonNode kakao = login("KAKAO", "accessToken");
        JsonNode google = login("GOOGLE", "idToken");

        assertThat(google.get("isNewMember").asBoolean()).isTrue();
        assertThat(google.get("member").get("email").asString()).isEqualTo(email);
        assertThat(google.get("member").get("id").asString()).isNotEqualTo(kakao.get("member").get("id").asString());
    }

    @Test
    void 동시에_처음_로그인해도_제공자_식별자에_계정이_하나만_생긴다() throws Exception {
        CountDownLatch verified = new CountDownLatch(2);
        when(kakaoVerifier.verify(any(SocialCredential.class))).thenAnswer(invocation -> {
            verified.countDown();
            if (!verified.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("두 로그인 요청이 모두 검증기에 도착해야 합니다.");
            }
            return new SocialIdentity(OAuthProvider.KAKAO, subject, email);
        });

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstRequest = executor.submit(() -> login("KAKAO", "accessToken"));
            var secondRequest = executor.submit(() -> login("KAKAO", "accessToken"));
            JsonNode first = firstRequest.get(30, TimeUnit.SECONDS);
            JsonNode second = secondRequest.get(30, TimeUnit.SECONDS);
            assertThat(first.get("member").get("id").asString()).isEqualTo(second.get("member").get("id").asString());
            assertThat(first.get("isNewMember").asBoolean() ^ second.get("isNewMember").asBoolean()).isTrue();
        }
    }

    @Test
    void 재발급하면_새_토큰_쌍을_받고_이전_리프레시_토큰은_다시_쓸_수_없다() throws Exception {
        JsonNode original = login("KAKAO", "accessToken");
        String oldRefresh = original.get("refreshToken").asString();

        JsonNode renewed = reissue(oldRefresh);
        assertThat(renewed.get("accessToken").asString()).isNotEqualTo(original.get("accessToken").asString());
        assertThat(renewed.get("refreshToken").asString()).isNotEqualTo(oldRefresh);
        assertThat(renewed.get("expiresIn").asLong()).isEqualTo(mobileAuthProperties.accessTokenExpirySeconds());
        rejectRefresh(oldRefresh);
    }

    @Test
    void 액세스_토큰이_만료되어도_리프레시_토큰으로_재발급할_수_있다() throws Exception {
        JsonNode login = login("KAKAO", "accessToken");
        Long userId = Long.valueOf(login.get("member").get("id").asString());
        String expired = new MobileAccessTokenService(
                new MobileAuthProperties(mobileAuthProperties.issuer(), mobileAuthProperties.accessTokenSecret(), -60,
                        mobileAuthProperties.refreshTokenExpirySeconds(), mobileAuthProperties.refreshTokenKeyPrefix()))
            .issue(userId);

        mvc.perform(get("/api/v2/unknown").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
        assertThat(reissue(login.get("refreshToken").asString()).get("accessToken").asString()).isNotBlank();
    }

    @Test
    void 로그아웃은_204를_반환하고_해당_기기의_토큰만_폐기한다() throws Exception {
        JsonNode deviceA = login("KAKAO", "accessToken");
        JsonNode deviceB = login("KAKAO", "accessToken");
        String tokenA = deviceA.get("refreshToken").asString();
        String tokenB = deviceB.get("refreshToken").asString();

        mvc.perform(post("/api/v2/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + deviceA.get("accessToken").asString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(Map.of("refreshToken", tokenA))))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
        rejectRefresh(tokenA);
        assertThat(reissue(tokenB).get("refreshToken").asString()).isNotEqualTo(tokenB);
    }

    @ParameterizedTest
    @EnumSource(value = MobileUserStatus.class, names = { "SUSPENDED", "WITHDRAWN" })
    void 사용할_수_없는_회원은_로그인과_재발급을_거부한다(MobileUserStatus status) throws Exception {
        JsonNode login = login("KAKAO", "accessToken");
        Long userId = Long.valueOf(login.get("member").get("id").asString());
        jdbcTemplate.update("UPDATE tb_v2_user SET status = ? WHERE id = ?", status.name(), userId);

        mvc.perform(post("/api/v2/auth/oauth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(Map.of("provider", "KAKAO", "accessToken", "fake-credential"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(post("/api/v2/auth/token/reissue").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(Map.of("refreshToken", login.get("refreshToken").asString()))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 지원하지_않는_제공자와_누락된_Google_ID_토큰은_400이다() throws Exception {
        mvc.perform(post("/api/v2/auth/oauth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(Map.of("provider", "APPLE"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        when(googleVerifier.verify(any(SocialCredential.class))).thenAnswer(invocation -> {
            SocialCredential credential = invocation.getArgument(0);
            if (credential.idToken() == null || credential.idToken().isBlank()) {
                throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "Google ID 토큰이 필요합니다.");
            }
            return new SocialIdentity(OAuthProvider.GOOGLE, subject, email);
        });
        mvc.perform(post("/api/v2/auth/oauth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(Map.of("provider", "GOOGLE"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 잘못된_소셜_인증_정보는_401_소셜_오류_코드로_응답한다() throws Exception {
        when(kakaoVerifier.verify(any(SocialCredential.class)))
            .thenThrow(new MobileException(MobileErrorCode.OAUTH_CREDENTIAL_INVALID, "소셜 로그인 정보를 확인할 수 없습니다."));

        mvc.perform(post("/api/v2/auth/oauth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(Map.of("provider", "KAKAO", "accessToken", "invalid"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("OAUTH_CREDENTIAL_INVALID"));
    }

    @Test
    void 액세스_토큰_없는_로그아웃은_v2_401_응답이다() throws Exception {
        mvc.perform(post("/api/v2/auth/logout").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(Map.of("refreshToken", "any-token"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.success").value(false));
    }

    private JsonNode reissue(String token) throws Exception {
        byte[] body = mvc
            .perform(post("/api/v2/auth/token/reissue").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("refreshToken", token))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        JsonNode data = mapper.readTree(body).get("data");
        issuedRefreshTokens.put(data.get("refreshToken").asString(), issuedRefreshTokens.get(token));
        return data;
    }

    private void rejectRefresh(String token) throws Exception {
        mvc.perform(post("/api/v2/auth/token/reissue").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(Map.of("refreshToken", token))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private JsonNode login(String provider, String credentialName) throws Exception {
        byte[] body = mvc
            .perform(post("/api/v2/auth/oauth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("provider", provider, credentialName, "fake-credential"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        JsonNode data = mapper.readTree(body).get("data");
        Long userId = Long.valueOf(data.get("member").get("id").asString());
        createdUserIds.add(userId);
        issuedRefreshTokens.put(data.get("refreshToken").asString(), userId);
        return data;
    }

}
