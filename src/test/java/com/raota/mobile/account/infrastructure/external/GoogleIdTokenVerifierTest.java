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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class GoogleIdTokenVerifierTest {

    private static KeyPair keyPair;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void 유효한_토큰은_제공자_식별자와_이메일을_반환한다() throws Exception {
        var identity = verifier(keyPair).verify(credential(
                token(keyPair, "https://accounts.google.com", "mobile-client", Instant.now().plusSeconds(300))));

        assertThat(identity.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(identity.subject()).isEqualTo("google-subject");
        assertThat(identity.email()).isEqualTo("member@example.com");
    }

    @Test
    void 다른_앱을_대상으로_하는_토큰은_거부한다() throws Exception {
        assertInvalid(
                credential(token(keyPair, "accounts.google.com", "foreign-client", Instant.now().plusSeconds(300))),
                keyPair);
    }

    @Test
    void 다른_발급자의_토큰은_거부한다() throws Exception {
        assertInvalid(
                credential(
                        token(keyPair, "https://untrusted.example", "mobile-client", Instant.now().plusSeconds(300))),
                keyPair);
    }

    @Test
    void 만료된_토큰은_거부한다() throws Exception {
        assertInvalid(credential(
                token(keyPair, "https://accounts.google.com", "mobile-client", Instant.now().minusSeconds(3600))),
                keyPair);
    }

    @Test
    void 서명이_다른_토큰은_거부한다() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair foreignKey = generator.generateKeyPair();
        assertInvalid(credential(
                token(foreignKey, "https://accounts.google.com", "mobile-client", Instant.now().plusSeconds(300))),
                keyPair);
    }

    @Test
    void ID_토큰을_누락하면_입력_오류다() {
        assertThatThrownBy(() -> verifier(keyPair).verify(new SocialCredential(null, null, null, null)))
            .isInstanceOfSatisfying(MobileException.class,
                    exception -> assertThat(exception.code()).isEqualTo(MobileErrorCode.VALIDATION_ERROR));
    }

    private static void assertInvalid(SocialCredential credential, KeyPair trustedKey) {
        assertThatThrownBy(() -> verifier(trustedKey).verify(credential)).isInstanceOfSatisfying(MobileException.class,
                exception -> assertThat(exception.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
    }

    private static GoogleIdTokenVerifier verifier(KeyPair key) {
        var properties = new MobileOAuthProperties(
                new MobileOAuthProperties.Google(List.of("mobile-client"), "https://unused.example/certs"),
                new MobileOAuthProperties.Kakao("123456", "https://kapi.kakao.com"));
        return new GoogleIdTokenVerifier(properties,
                NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) key.getPublic()).build());
    }

    private static SocialCredential credential(String idToken) {
        return new SocialCredential(idToken, null, null, null);
    }

    private static String token(KeyPair key, String issuer, String audience, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer)
            .audience(audience)
            .subject("google-subject")
            .claim("email", "member@example.com")
            .issueTime(Date.from(Instant.now().minusSeconds(30)))
            .expirationTime(Date.from(expiresAt))
            .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(key.getPrivate()));
        return jwt.serialize();
    }

}
