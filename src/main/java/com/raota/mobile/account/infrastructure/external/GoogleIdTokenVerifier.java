package com.raota.mobile.account.infrastructure.external;

import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.application.port.SocialTokenVerifier;
import com.raota.mobile.account.application.result.SocialIdentity;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.time.Duration;
import java.util.Set;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Google의 공개 키와 토큰의 시각·발급자·대상을 함께 검증한다. 이메일은 Google이 인증한 경우에만 쓴다. */
@Component
public class GoogleIdTokenVerifier implements SocialTokenVerifier {

    private static final String INVALID_CREDENTIAL_MESSAGE = "소셜 로그인 정보를 확인할 수 없습니다.";

    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder decoder;

    private final OAuth2TokenValidator<Jwt> validator;

    @Autowired
    public GoogleIdTokenVerifier(MobileOAuthProperties properties) {
        this(properties, NimbusJwtDecoder.withJwkSetUri(properties.google().jwkSetUri()).build());
    }

    GoogleIdTokenVerifier(MobileOAuthProperties properties, JwtDecoder decoder) {
        this.decoder = decoder;
        this.validator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
                jwt -> validate(ISSUERS.contains(jwt.getClaimAsString("iss"))),
                jwt -> validate(jwt.getAudience() != null
                        && jwt.getAudience().stream().anyMatch(properties.google().clientIds()::contains)),
                jwt -> validate(jwt.getSubject() != null && !jwt.getSubject().isBlank()));
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public SocialIdentity verify(SocialCredential credential) {
        if (credential.idToken() == null || credential.idToken().isBlank()) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "Google ID 토큰이 필요합니다.");
        }
        try {
            Jwt jwt = decoder.decode(credential.idToken());
            if (validator.validate(jwt).hasErrors()) {
                throw invalidCredential();
            }
            String email = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")) ? jwt.getClaimAsString("email")
                    : null;
            return new SocialIdentity(provider(), jwt.getSubject(), email);
        }
        catch (JwtException exception) {
            throw invalidCredential();
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
