package com.raota.mobile.account.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.application.port.RefreshTokenStore;
import com.raota.mobile.account.application.result.SocialIdentity;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.account.domain.repository.MobileUserOAuthAccountRepository;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.external.GoogleIdTokenVerifier;
import com.raota.mobile.account.infrastructure.external.KakaoAccessTokenVerifier;
import com.raota.support.BaseIntegrationTest;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
