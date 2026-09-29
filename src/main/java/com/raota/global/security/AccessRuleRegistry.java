package com.raota.global.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class AccessRuleRegistry {

    private final Map<AccessLevel, RequestMatcher[]> matchers;

    public AccessRuleRegistry(List<AccessRuleContributor> contributors) {
        List<AccessRule> rules = contributors.stream()
            .flatMap(contributor -> contributor.accessRules().stream())
            .toList();
        Map<AccessLevel, RequestMatcher[]> matchers = new EnumMap<>(AccessLevel.class);
        for (AccessLevel level : AccessLevel.values()) {
            matchers.put(level,
                    rules.stream()
                        .filter(rule -> rule.level() == level)
                        .map(AccessRule::matcher)
                        .toArray(RequestMatcher[]::new));
        }
        this.matchers = Map.copyOf(matchers);
    }

    public RequestMatcher[] matchersFor(AccessLevel level) {
        return matchers.get(level).clone();
    }

    public Set<AccessLevel> matchingAccessLevels(HttpServletRequest request) {
        EnumSet<AccessLevel> matches = EnumSet.noneOf(AccessLevel.class);
        for (AccessLevel level : AccessLevel.values()) {
            for (RequestMatcher matcher : matchers.get(level)) {
                if (matcher.matches(request)) {
                    matches.add(level);
                    break;
                }
            }
        }
        return Set.copyOf(matches);
    }

}
