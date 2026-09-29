package com.raota.mobile.account.infrastructure.external;

import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.application.port.SocialTokenVerifier;
import com.raota.mobile.account.application.result.SocialIdentity;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/** Kakao 토큰의 앱 소유권을 확인한 후 제공자의 회원 식별자를 조회한다. */
@Component
public class KakaoAccessTokenVerifier implements SocialTokenVerifier {

    private static final String INVALID_CREDENTIAL_MESSAGE = "소셜 로그인 정보를 확인할 수 없습니다.";

    private final RestClient client;

    private final String appId;

    public KakaoAccessTokenVerifier(RestClient.Builder builder, MobileOAuthProperties properties) {
        this.client = builder.baseUrl(properties.kakao().apiBaseUrl()).build();
        this.appId = properties.kakao().appId();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public SocialIdentity verify(SocialCredential credential) {
        String token = credential.accessToken();
        if (token == null || token.isBlank()) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "Kakao 액세스 토큰이 필요합니다.");
        }

        try {
            JsonNode info = client.get()
                .uri("/v1/user/access_token_info")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(JsonNode.class);
            if (info == null || info.get("app_id") == null || !appId.equals(info.get("app_id").asString())) {
                throw invalidCredential();
            }

            JsonNode profile = client.get()
                .uri("/v2/user/me")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(JsonNode.class);
            if (profile == null || profile.get("id") == null || profile.get("id").isNull()) {
                throw invalidCredential();
            }
            JsonNode account = profile.get("kakao_account");
            JsonNode email = account == null ? null : account.get("email");
            return new SocialIdentity(provider(), profile.get("id").asString(),
                    email == null || email.isNull() ? null : email.asString());
        }
        catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw invalidCredential();
            }
            throw exception;
        }
    }

    private static MobileException invalidCredential() {
        return new MobileException(MobileErrorCode.OAUTH_CREDENTIAL_INVALID, INVALID_CREDENTIAL_MESSAGE);
    }

}
