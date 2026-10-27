package com.raota.agent.presentation.mobile;

import com.raota.global.security.AccessLevel;
import com.raota.global.security.AccessRule;
import com.raota.global.security.AccessRuleContributor;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** agent가 제공하는 v2 API(AI 추천, 오늘의 큐레이션)의 접근 등급을 등록한다. */
@Component
public class MobileAgentAccessRules implements AccessRuleContributor {

    @Override
    public List<AccessRule> accessRules() {
        return List.of(
                new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/api/v2/ai/recommendations"),
                new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/api/v2/ai/recommendations/today"),
                new AccessRule(
                        AccessLevel.AUTHENTICATED,
                        HttpMethod.POST,
                        "/api/v2/ai/recommendations/{recommendationId}/shops/{shopId}/open"),
                new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v2/curations/today"));
    }
}
