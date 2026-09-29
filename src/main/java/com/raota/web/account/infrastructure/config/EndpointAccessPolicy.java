package com.raota.web.account.infrastructure.config;

import com.raota.global.security.AccessLevel;
import com.raota.global.security.AccessRule;
import com.raota.global.security.AccessRuleContributor;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class EndpointAccessPolicy implements AccessRuleContributor {

    private static final List<AccessRule> RULES = List.of(
            // Framework and operational endpoints
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.OPTIONS, "/**"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/login"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/oauth2/authorization/**"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/login/oauth2/code/**"),
            new AccessRule(AccessLevel.PUBLIC, null, "/error"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/swagger-ui.html"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/swagger-ui/**"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/v3/api-docs"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/v3/api-docs.yaml"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/v3/api-docs/**"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/actuator/health"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/actuator/health/**"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/actuator/prometheus"),

            // Public application endpoints
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.POST, "/auth/refresh"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.POST, "/auth/logout"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/favicon.ico"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/community/posts"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/community/posts/{postId:[0-9]+}"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/community/posts/{postId:[0-9]+}/comments"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/community/ramen-shops"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.POST, "/community/posts/{postId:[0-9]+}/views"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/community/posts"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/community/posts/popular"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/profile"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/photos"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/visits"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/posts"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/comments"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/ramen-logs"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/ramen-logs/shops"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops/{shopId:[0-9]+}"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops/{shopId:[0-9]+}/menus"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/views"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops/{shopId:[0-9]+}/votes"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops/{shopId:[0-9]+}/photos"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/shops/recent-verified"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-logs"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-logs/{logId:[0-9]+}"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/discovery/popular-shops/today"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/discovery/today-recommendations"),
            new AccessRule(AccessLevel.PUBLIC, HttpMethod.PUT, "/files/mock-upload-endpoint"),

            // Authenticated member endpoints
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/community/posts"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/community/posts/{postId:[0-9]+}"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/community/posts/{postId:[0-9]+}"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/community/posts/{postId:[0-9]+}/likes"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/community/posts/{postId:[0-9]+}/comments"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.PUT, "/community/comments/{commentId:[0-9]+}"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/community/comments/{commentId:[0-9]+}"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/summary"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/profile"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/users/me/profile"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/users/me/email"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/privacy-settings"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/users/me/privacy-settings"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/users/me"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/photos"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/bookmarks"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/visits"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/posts"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/comments"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/ramen-logs"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/ramen-logs/shops"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/bookmark"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/reports"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST,
                    "/ramen-shops/{shopId:[0-9]+}/votes/menus/{menuId:[0-9]+}"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/photos"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE,
                    "/ramen-shops/{shopId:[0-9]+}/photos/{photoId:[0-9]+}"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/ai-search"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/compare"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-logs"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/ramen-logs/{logId:[0-9]+}"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/ramen-logs/{logId:[0-9]+}"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-logs/{logId:[0-9]+}/likes"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/recommendations/summary"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/recommendations/chat"),
            new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/files/upload-ticket"),

            // Administrative and operational endpoints
            new AccessRule(AccessLevel.ADMIN, null, "/admin/**"),
            new AccessRule(AccessLevel.ADMIN, HttpMethod.POST, "/api/v1/discovery/today-recommendations/generate"),
            new AccessRule(AccessLevel.ADMIN, null, "/actuator/**"));

    @Override
    public List<AccessRule> accessRules() {
        return RULES;
    }

}
