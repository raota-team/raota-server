package com.raota.mobile.account.infrastructure.external;

import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.application.port.SocialTokenVerifier;
import com.raota.mobile.account.application.result.SocialIdentity;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
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

/** Apple의 공개 키, 발급자, 앱 식별자와 요청에 사용한 nonce를 검증한다. */
@Component
public class AppleIdentityTokenVerifier implements SocialTokenVerifier {

    private static final String INVALID_CREDENTIAL_MESSAGE = "소셜 로그인 정보를 확인할 수 없습니다.";

    private final JwtDecoder decoder;

    private final OAuth2TokenValidator<Jwt> validator;

    @Autowired
    public AppleIdentityTokenVerifier(MobileOAuthProperties properties) {
        this(properties, NimbusJwtDecoder.withJwkSetUri(properties.apple().jwkSetUri()).build());
    }

    AppleIdentityTokenVerifier(MobileOAuthProperties properties, JwtDecoder decoder) {
        this.decoder = decoder;
        this.validator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
                jwt -> validate(properties.apple().issuer().equals(jwt.getClaimAsString("iss"))),
                jwt -> validate(jwt.getAudience() != null && jwt.getAudience().contains(properties.apple().bundleId())),
                jwt -> validate(jwt.getSubject() != null && !jwt.getSubject().isBlank()));
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.APPLE;
    }

    @Override
    public SocialIdentity verify(SocialCredential credential) {
        if (credential.idToken() == null || credential.idToken().isBlank() || credential.nonce() == null
                || credential.nonce().isBlank()) {
            throw invalidCredential();
        }
        try {
            Jwt jwt = decoder.decode(credential.idToken());
            if (validator.validate(jwt).hasErrors() || !validNonce(credential.nonce(), jwt.getClaimAsString("nonce"))) {
                throw invalidCredential();
            }
            Object emailVerified = jwt.getClaim("email_verified");
            String email = Boolean.TRUE.equals(emailVerified) || "true".equals(emailVerified)
                    ? jwt.getClaimAsString("email") : null;
            return new SocialIdentity(provider(), jwt.getSubject(), email);
        }
        catch (JwtException exception) {
            throw invalidCredential();
        }
    }

    private static boolean validNonce(String rawNonce, String claim) {
        if (claim == null) {
            return false;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawNonce.getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().formatHex(digest).getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, claim.getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private static OAuth2TokenValidatorResult validate(boolean valid) {
        return valid ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    }

    private static MobileException invalidCredential() {
        return new MobileException(MobileErrorCode.OAUTH_CREDENTIAL_INVALID, INVALID_CREDENTIAL_MESSAGE);
    }

}
