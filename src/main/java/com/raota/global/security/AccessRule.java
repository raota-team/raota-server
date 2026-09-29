package com.raota.global.security;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public record AccessRule(AccessLevel level, HttpMethod method, String pattern) {

    public RequestMatcher matcher() {
        if (method == null) {
            return pathPattern(pattern);
        }
        if (level == AccessLevel.PUBLIC && method == HttpMethod.GET) {
            return new OrRequestMatcher(pathPattern(HttpMethod.GET, pattern), pathPattern(HttpMethod.HEAD, pattern));
        }
        return pathPattern(method, pattern);
    }

    @Override
    public String toString() {
        return "%s %s -> %s".formatted(method == null ? "*" : method, pattern, level);
    }

}
