package com.raota.mobile.account.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class AppleTokenRestClientTest {

    private static KeyPair keyPair;

    private static KeyPair rsaKeyPair;

    private MockRestServiceServer server;

    private AppleTokenRestClient client;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = generator.generateKeyPair();
        KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance("RSA");
        rsaGenerator.initialize(2048);
        rsaKeyPair = rsaGenerator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AppleTokenRestClient(builder, properties(bareKey()), decoder());
    }

    @Test
    void 인가_코드를_제출하고_ES256_클라이언트_JWT로_인증한다() throws Exception {
        server.expect(requestTo("https://apple.example/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(request -> {
                Map<String, String> form = form((MockClientHttpRequest) request);
                assertThat(form).containsEntry("grant_type", "authorization_code")
                    .containsEntry("code", "authorization-code")
                    .containsEntry("client_id", "net.raota.mobile.test");
                verifyClientSecret(form.get("client_secret"));
            })
            .andRespond(withSuccess(
                    "{\"refresh_token\":\"refresh-secret\",\"id_token\":\"" + idToken("https://appleid.apple.com",
                            "net.raota.mobile.test", "apple-subject", Instant.now().plusSeconds(300)) + "\"}",
                    MediaType.APPLICATION_JSON));

        assertThat(client.exchange("authorization-code", "apple-subject")).isEqualTo("refresh-secret");
        server.verify();
    }

    @Test
    void PEM_줄바꿈_방식에_관계없이_서명_키를_읽는다() throws Exception {
        String pem = "-----BEGIN PRIVATE KEY-----\n" + bareKey() + "\n-----END PRIVATE KEY-----";
        for (String key : new String[] { pem, pem.replace("\n", "\\n") }) {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer mock = MockRestServiceServer.bindTo(builder).build();
            AppleTokenRestClient tokenClient = new AppleTokenRestClient(builder, properties(key), decoder());
            mock.expect(requestTo("https://apple.example/revoke"))
                .andExpect(request -> verifyClientSecret(form((MockClientHttpRequest) request).get("client_secret")))
                .andRespond(withSuccess());
            tokenClient.revoke("revoked-token");
            mock.verify();
        }
    }

    @Test
    void 토큰_폐기는_제공자에게_갱신_토큰을_제출한다() {
        server.expect(requestTo("https://apple.example/revoke"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(request -> assertThat(form((MockClientHttpRequest) request))
                .containsEntry("token", "refresh-secret")
                .containsEntry("token_type_hint", "refresh_token")
                .containsEntry("client_id", "net.raota.mobile.test"))
            .andRespond(withSuccess());

        client.revoke("refresh-secret");
        server.verify();
    }

    @Test
    void 잘못된_코드나_갱신_토큰_누락은_401이다() {
        server.expect(requestTo("https://apple.example/token"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest());
        server.expect(requestTo("https://apple.example/token"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.exchange("invalid", "apple-subject")).isInstanceOfSatisfying(
                MobileException.class,
                error -> assertThat(error.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
        assertThatThrownBy(() -> client.exchange("missing", "apple-subject")).isInstanceOfSatisfying(
                MobileException.class,
                error -> assertThat(error.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
        server.verify();
    }

    @Test
    void Apple_서버_오류는_재시도를_위해_상위로_전파한다() {
        server.expect(requestTo("https://apple.example/token")).andRespond(withServerError());
        server.expect(requestTo("https://apple.example/revoke")).andRespond(withServerError());
        assertThatThrownBy(() -> client.exchange("code", "apple-subject"))
            .isInstanceOf(RestClientResponseException.class);
        assertThatThrownBy(() -> client.revoke("token")).isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    void 잘못된_개인키는_초기화할_수_없다() {
        assertThatThrownBy(() -> new AppleTokenRestClient(RestClient.builder(), properties("invalid-key"), decoder()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 다른_회원의_코드는_발급된_토큰을_폐기한_뒤_거부한다() throws Exception {
        server.expect(requestTo("https://apple.example/token"))
            .andRespond(withSuccess(
                    "{\"refresh_token\":\"other-refresh\",\"id_token\":\"" + idToken("https://appleid.apple.com",
                            "net.raota.mobile.test", "different-subject", Instant.now().plusSeconds(300)) + "\"}",
                    MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://apple.example/revoke"))
            .andExpect(
                    request -> assertThat(form((MockClientHttpRequest) request)).containsEntry("token", "other-refresh")
                        .containsEntry("token_type_hint", "refresh_token"))
            .andRespond(withSuccess());

        assertThatThrownBy(() -> client.exchange("other-code", "apple-subject")).isInstanceOfSatisfying(
                MobileException.class,
                error -> assertThat(error.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
        server.verify();
    }

    @Test
    void 토큰_응답에_ID_토큰이_없어도_발급된_토큰을_폐기한다() {
        server.expect(requestTo("https://apple.example/token"))
            .andRespond(withSuccess("{\"refresh_token\":\"orphan-refresh\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://apple.example/revoke"))
            .andExpect(request -> assertThat(form((MockClientHttpRequest) request)).containsEntry("token",
                    "orphan-refresh"))
            .andRespond(withSuccess());

        assertThatThrownBy(() -> client.exchange("code", "apple-subject")).isInstanceOfSatisfying(MobileException.class,
                error -> assertThat(error.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
        server.verify();
    }

    private static NimbusJwtDecoder decoder() {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) rsaKeyPair.getPublic()).build();
    }

    private static String idToken(String issuer, String audience, String subject, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer)
            .audience(audience)
            .subject(subject)
            .issueTime(Date.from(Instant.now().minusSeconds(30)))
            .expirationTime(Date.from(expiresAt))
            .build();
        SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        token.sign(new RSASSASigner(rsaKeyPair.getPrivate()));
        return token.serialize();
    }

    private static void verifyClientSecret(String raw) {
        try {
            SignedJWT jwt = SignedJWT.parse(raw);
            assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
            assertThat(jwt.getHeader().getKeyID()).isEqualTo("TESTKEY001");
            assertThat(jwt.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic()))).isTrue();
            var claims = jwt.getJWTClaimsSet();
            assertThat(claims.getIssuer()).isEqualTo("TESTTEAM01");
            assertThat(claims.getAudience()).containsExactly("https://appleid.apple.com");
            assertThat(claims.getSubject()).isEqualTo("net.raota.mobile.test");
            assertThat(claims.getIssueTime().toInstant()).isBetween(Instant.now().minusSeconds(10), Instant.now());
            assertThat(claims.getExpirationTime().getTime() - claims.getIssueTime().getTime()).isEqualTo(300_000);
        }
        catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static Map<String, String> form(MockClientHttpRequest request) {
        return Arrays.stream(request.getBodyAsString().split("&"))
            .map(field -> field.split("=", 2))
            .collect(Collectors.toMap(field -> URLDecoder.decode(field[0], StandardCharsets.UTF_8),
                    field -> URLDecoder.decode(field[1], StandardCharsets.UTF_8)));
    }

    private static String bareKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    private static MobileOAuthProperties properties(String key) {
        return new MobileOAuthProperties(new MobileOAuthProperties.Google(List.of("google"), "https://unused.example"),
                new MobileOAuthProperties.Kakao("123456", "https://unused.example"),
                new MobileOAuthProperties.Apple("net.raota.mobile.test", "TESTTEAM01", "TESTKEY001", key,
                        "cm2PatRyO0rn/bD+j2wAupkmWGwmDGXdnq3PxZx59Fk=", "https://unused.example/keys",
                        "https://appleid.apple.com", "https://apple.example/token", "https://apple.example/revoke"));
    }

}
