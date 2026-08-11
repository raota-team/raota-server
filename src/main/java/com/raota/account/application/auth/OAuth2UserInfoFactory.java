package com.raota.account.application.auth;

import com.raota.account.domain.auth.model.AuthProvider;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class OAuth2UserInfoFactory {

    public OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        return switch (provider) {
            case GOOGLE -> fromGoogle(attributes);
            case KAKAO -> fromKakao(attributes);
        };
    }

    private OAuth2UserInfo fromGoogle(Map<String, Object> attributes) {
        String providerUserId = getString(attributes, "sub");
        String nickname = getString(attributes, "name");
        String email = getString(attributes, "email");
        String profileImageUrl = getString(attributes, "picture");
        return new OAuth2UserInfo(AuthProvider.GOOGLE, providerUserId, email, nickname, profileImageUrl);
    }

    @SuppressWarnings("unchecked")
    private OAuth2UserInfo fromKakao(Map<String, Object> attributes) {
        Object id = attributes.get("id");
        if (id == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), "카카오 사용자 ID가 없습니다.");
        }

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());
        String providerUserId = String.valueOf(id);
        String email = getString(kakaoAccount, "email");
        String nickname = getString(profile, "nickname");
        String profileImageUrl = getString(profile, "profile_image_url");
        return new OAuth2UserInfo(AuthProvider.KAKAO, providerUserId, email, nickname, profileImageUrl);
    }

    private String getString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
