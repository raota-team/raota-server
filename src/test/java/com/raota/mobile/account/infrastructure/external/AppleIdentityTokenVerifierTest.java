package com.raota.mobile.account.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class AppleIdentityTokenVerifierTest {

    private static final String RAW_NONCE = "random-app-nonce";

    private static KeyPair keyPair;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void 유효한_토큰의_식별자와_검증된_이메일을_사용한다() throws Exception {
        var identity = verifier().verify(credential(token("https://appleid.apple.com", "net.raota.mobile.test",
                Instant.now().plusSeconds(300), nonceHash(), "true", keyPair)));

        assertThat(identity.provider()).isEqualTo(OAuthProvider.APPLE);
        assertThat(identity.subject()).isEqualTo("apple-subject");
        assertThat(identity.email()).isEqualTo("member@example.com");
        var booleanIdentity = verifier().verify(credential(token("https://appleid.apple.com", "net.raota.mobile.test",
                Instant.now().plusSeconds(300), nonceHash(), true, keyPair)));
        assertThat(booleanIdentity.email()).isEqualTo("member@example.com");
    }

    @Test
    void 다른_앱_발급자_만료_서명은_거부한다() throws Exception {
        assertInvalid(token("https://appleid.apple.com", "other.bundle", Instant.now().plusSeconds(300), nonceHash(),
                true, keyPair));
        assertInvalid(token("https://foreign.example", "net.raota.mobile.test", Instant.now().plusSeconds(300),
                nonceHash(), true, keyPair));
        assertInvalid(token("https://appleid.apple.com", "net.raota.mobile.test", Instant.now().minusSeconds(3600),
                nonceHash(), true, keyPair));
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        assertInvalid(token("https://appleid.apple.com", "net.raota.mobile.test", Instant.now().plusSeconds(300),
                nonceHash(), true, generator.generateKeyPair()));
    }

    @Test
    void 해시가_다르거나_없는_nonce를_거부한다() throws Exception {
        assertInvalid(token("https://appleid.apple.com", "net.raota.mobile.test", Instant.now().plusSeconds(300),
                "different", true, keyPair));
        assertInvalid(token("https://appleid.apple.com", "net.raota.mobile.test", Instant.now().plusSeconds(300), null,
                true, keyPair));
        String valid = token("https://appleid.apple.com", "net.raota.mobile.test", Instant.now().plusSeconds(300),
                nonceHash(), true, keyPair);
        assertThatThrownBy(() -> verifier().verify(new SocialCredential(valid, null, "code", " ")))
            .isInstanceOfSatisfying(MobileException.class,
                    error -> assertThat(error.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
        assertThatThrownBy(() -> verifier().verify(new SocialCredential(null, null, "code", RAW_NONCE)))
            .isInstanceOfSatisfying(MobileException.class,
                    error -> assertThat(error.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
    }

    @Test
    void 미인증_이메일은_사용하지_않는다() throws Exception {
        for (Object verified : new Object[] { false, "false", null }) {
            String token = token("https://appleid.apple.com", "net.raota.mobile.test", Instant.now().plusSeconds(300),
                    nonceHash(), verified, keyPair);
            assertThat(verifier().verify(credential(token)).email()).isNull();
        }
    }

    private static void assertInvalid(String token) {
        assertThatThrownBy(() -> verifier().verify(credential(token))).isInstanceOfSatisfying(MobileException.class,
                error -> assertThat(error.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
    }

    private static AppleIdentityTokenVerifier verifier() {
        var properties = new MobileOAuthProperties(
                new MobileOAuthProperties.Google(List.of("mobile-client"), "https://unused.example/certs"),
                new MobileOAuthProperties.Kakao("123456", "https://kakao.example"),
                new MobileOAuthProperties.Apple("net.raota.mobile.test", "TESTTEAM01", "TESTKEY001", "test-key",
                        "cm2PatRyO0rn/bD+j2wAupkmWGwmDGXdnq3PxZx59Fk=", "https://unused.example/certs",
                        "https://appleid.apple.com", "https://unused.example/token", "https://unused.example/revoke"));
        return new AppleIdentityTokenVerifier(properties,
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build());
    }

    private static SocialCredential credential(String token) {
        return new SocialCredential(token, null, "code", RAW_NONCE);
    }

    private static String nonceHash() throws Exception {
        return HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-256").digest(RAW_NONCE.getBytes(StandardCharsets.UTF_8)));
    }

    private static String token(String issuer, String audience, Instant expiry, String nonce, Object verified,
            KeyPair signer) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(issuer)
            .audience(audience)
            .subject("apple-subject")
            .claim("email", "member@example.com")
            .issueTime(Date.from(Instant.now().minusSeconds(30)))
            .expirationTime(Date.from(expiry));
        if (nonce != null) {
            claims.claim("nonce", nonce);
        }
        if (verified != null) {
            claims.claim("email_verified", verified);
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
        jwt.sign(new RSASSASigner(signer.getPrivate()));
        return jwt.serialize();
    }

}
