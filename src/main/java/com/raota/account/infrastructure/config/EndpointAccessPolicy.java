package com.raota.account.infrastructure.config;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public final class EndpointAccessPolicy {

    public enum AccessLevel {
        PUBLIC,
        AUTHENTICATED,
        ADMIN
    }

    private static final List<Rule> RULES = List.of(
            // Framework and operational endpoints
            rule(AccessLevel.PUBLIC, HttpMethod.OPTIONS, "/**"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/login"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/oauth2/authorization/**"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/login/oauth2/code/**"),
            rule(AccessLevel.PUBLIC, null, "/error"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/swagger-ui.html"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/swagger-ui/**"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/v3/api-docs"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/v3/api-docs.yaml"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/v3/api-docs/**"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/actuator/health"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/actuator/health/**"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/actuator/prometheus"),

            // Public application endpoints
            rule(AccessLevel.PUBLIC, HttpMethod.POST, "/auth/refresh"),
            rule(AccessLevel.PUBLIC, HttpMethod.POST, "/auth/logout"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/favicon.ico"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/community/posts"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/community/posts/{postId:[0-9]+}"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/community/posts/{postId:[0-9]+}/comments"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/community/ramen-shops"),
            rule(AccessLevel.PUBLIC, HttpMethod.POST, "/community/posts/{postId:[0-9]+}/views"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/community/posts"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/community/posts/popular"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/profile"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/photos"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/visits"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/posts"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/comments"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/ramen-logs"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/users/{userId:[0-9]+}/ramen-logs/shops"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops/{shopId:[0-9]+}"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops/{shopId:[0-9]+}/menus"),
            rule(AccessLevel.PUBLIC, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/views"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops/{shopId:[0-9]+}/votes"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-shops/{shopId:[0-9]+}/photos"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/shops/recent-verified"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-logs"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/ramen-logs/{logId:[0-9]+}"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/discovery/stats"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/discovery/popular-shops/today"),
            rule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v1/discovery/today-recommendations"),
            rule(AccessLevel.PUBLIC, HttpMethod.PUT, "/files/mock-upload-endpoint"),

            // Authenticated member endpoints
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/community/posts"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/community/posts/{postId:[0-9]+}"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/community/posts/{postId:[0-9]+}"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/community/posts/{postId:[0-9]+}/likes"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/community/posts/{postId:[0-9]+}/comments"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.PUT, "/community/comments/{commentId:[0-9]+}"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/community/comments/{commentId:[0-9]+}"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/summary"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/profile"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/users/me/profile"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/users/me/email"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/privacy-settings"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/users/me/privacy-settings"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/users/me"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/photos"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/bookmarks"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/visits"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/posts"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/comments"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/ramen-logs"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/users/me/ramen-logs/shops"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/bookmark"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/reports"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/votes/menus/{menuId:[0-9]+}"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/{shopId:[0-9]+}/photos"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/ramen-shops/{shopId:[0-9]+}/photos/{photoId:[0-9]+}"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/ai-search"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-shops/compare"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-logs"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.PATCH, "/ramen-logs/{logId:[0-9]+}"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/ramen-logs/{logId:[0-9]+}"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/ramen-logs/{logId:[0-9]+}/likes"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/recommendations/summary"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/recommendations/chat"),
            rule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/files/upload-ticket"),

            // Administrative and operational endpoints
            rule(AccessLevel.ADMIN, null, "/admin/**"),
            rule(AccessLevel.ADMIN, HttpMethod.POST, "/api/v1/discovery/today-recommendations/generate"),
            rule(AccessLevel.ADMIN, null, "/actuator/**")
    );

    private EndpointAccessPolicy() {
    }

    static RequestMatcher[] matchersFor(AccessLevel accessLevel) {
        return RULES.stream()
                .filter(rule -> rule.accessLevel() == accessLevel)
                .map(Rule::matcher)
                .toArray(RequestMatcher[]::new);
    }

    public static Set<AccessLevel> matchingAccessLevels(HttpServletRequest request) {
        EnumSet<AccessLevel> matches = EnumSet.noneOf(AccessLevel.class);
        RULES.stream()
                .filter(rule -> rule.matcher().matches(request))
                .map(Rule::accessLevel)
                .forEach(matches::add);
        return Set.copyOf(matches);
    }

    private static Rule rule(AccessLevel accessLevel, HttpMethod method, String pattern) {
        RequestMatcher matcher;
        if (method == null) {
            matcher = pathPattern(pattern);
        } else if (accessLevel == AccessLevel.PUBLIC && method == HttpMethod.GET) {
            matcher = new OrRequestMatcher(
                    pathPattern(HttpMethod.GET, pattern),
                    pathPattern(HttpMethod.HEAD, pattern)
            );
        } else {
            matcher = pathPattern(method, pattern);
        }
        return new Rule(accessLevel, method, pattern, matcher);
    }

    record Rule(
            AccessLevel accessLevel,
            HttpMethod method,
            String pattern,
            RequestMatcher matcher
    ) {
        @Override
        public String toString() {
            return "%s %s -> %s".formatted(method == null ? "*" : method, pattern, accessLevel);
        }
    }
}
