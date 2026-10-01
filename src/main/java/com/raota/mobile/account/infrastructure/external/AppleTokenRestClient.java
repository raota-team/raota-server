package com.raota.mobile.account.infrastructure.external;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.raota.mobile.account.application.port.AppleTokenClient;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/** Apple OAuth 서버에 ES256 클라이언트 인증 JWT를 제출한다. */
@Component
public class AppleTokenRestClient implements AppleTokenClient {

    private static final String INVALID_CREDENTIAL_MESSAGE = "소셜 로그인 정보를 확인할 수 없습니다.";

    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final RestClient client;

    private final MobileOAuthProperties.Apple settings;

    private final ECPrivateKey signingKey;

    private final JwtDecoder identityDecoder;

    private final OAuth2TokenValidator<Jwt> identityValidator;

    @Autowired
    public AppleTokenRestClient(RestClient.Builder builder, MobileOAuthProperties properties) {
        this(builder, properties, NimbusJwtDecoder.withJwkSetUri(properties.apple().jwkSetUri()).build());
    }

    AppleTokenRestClient(RestClient.Builder builder, MobileOAuthProperties properties, JwtDecoder identityDecoder) {
        this.client = builder.build();
        this.settings = properties.apple();
        this.signingKey = parseKey(settings.privateKey());
        this.identityDecoder = identityDecoder;
        this.identityValidator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
                jwt -> validate(settings.issuer().equals(jwt.getClaimAsString("iss"))),
                jwt -> validate(jwt.getAudience() != null && jwt.getAudience().contains(settings.bundleId())));
    }

    @Override
    public String exchange(String authorizationCode, String expectedSubject) {
        MultiValueMap<String, String> form = credentials();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        try {
            JsonNode response = client.post()
                .uri(settings.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
            JsonNode refreshNode = response == null ? null : response.get("refresh_token");
            if (refreshNode == null || refreshNode.isNull() || refreshNode.asString().isBlank()) {
                throw invalidCredential();
            }
            String refreshToken = refreshNode.asString();
            JsonNode idToken = response.get("id_token");
            if (idToken == null || idToken.isNull() || !validIdentity(idToken.asString(), expectedSubject)) {
                try {
                    revoke(refreshToken);
                }
                catch (Exception ignored) {
                    // 이미 발급된 토큰 폐기는 최대한 시도하고, 잘못된 인증 응답은 그대로 거부한다.
                }
                throw invalidCredential();
            }
            return refreshToken;
        }
        catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw invalidCredential();
            }
            throw exception;
        }
    }

    private boolean validIdentity(String idToken, String expectedSubject) {
        if (idToken.isBlank()) {
            return false;
        }
        try {
            Jwt jwt = identityDecoder.decode(idToken);
            return !identityValidator.validate(jwt).hasErrors() && expectedSubject.equals(jwt.getSubject());
        }
        catch (JwtException exception) {
            return false;
        }
    }

    private static OAuth2TokenValidatorResult validate(boolean valid) {
        return valid ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    }

    @Override
    public void revoke(String refreshToken) {
        MultiValueMap<String, String> form = credentials();
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        client.post()
            .uri(settings.revokeUri())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .toBodilessEntity();
    }

    private MultiValueMap<String, String> credentials() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", settings.bundleId());
        form.add("client_secret", clientSecret());
        return form;
    }

    private String clientSecret() {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(settings.teamId())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(300)))
            .audience(APPLE_ISSUER)
            .subject(settings.bundleId())
            .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(settings.keyId()).build(),
                claims);
        try {
            jwt.sign(new ECDSASigner(signingKey));
            return jwt.serialize();
        }
        catch (JOSEException exception) {
            throw new IllegalStateException("Apple 클라이언트 서명에 실패했습니다.", exception);
        }
    }

    private static ECPrivateKey parseKey(String rawKey) {
        try {
            String base64 = rawKey.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            ECPrivateKey key = (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec p256 = parameters.getParameterSpec(ECParameterSpec.class);
            if (!key.getParams().getCurve().equals(p256.getCurve())
                    || !key.getParams().getGenerator().equals(p256.getGenerator())
                    || !key.getParams().getOrder().equals(p256.getOrder())) {
                throw new IllegalArgumentException("서명 키가 P-256 곡선을 사용하지 않습니다.");
            }
            return key;
        }
        catch (Exception exception) {
            throw new IllegalStateException("Apple PKCS#8 P-256 서명 키가 올바르지 않습니다.", exception);
        }
    }

    private static MobileException invalidCredential() {
        return new MobileException(MobileErrorCode.OAUTH_CREDENTIAL_INVALID, INVALID_CREDENTIAL_MESSAGE);
    }

}
