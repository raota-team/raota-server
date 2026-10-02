package com.raota.mobile.ramenlog.presentation;

import com.raota.global.security.AccessLevel;
import com.raota.global.security.AccessRule;
import com.raota.global.security.AccessRuleContributor;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** 라멘 기록과 업로드 API의 접근 등급을 등록한다. */
@Component
public class MobileRamenLogAccessRules implements AccessRuleContributor {

    @Override
    public List<AccessRule> accessRules() {
        return List.of(new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/api/v2/files/upload-tickets"),
                new AccessRule(AccessLevel.ACTIVE_MEMBER, HttpMethod.POST, "/api/v2/ramen-logs"),
                new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v2/ramen-logs/{logId:[0-9]+}"),
                new AccessRule(AccessLevel.ACTIVE_MEMBER, HttpMethod.PATCH, "/api/v2/ramen-logs/{logId:[0-9]+}"),
                new AccessRule(AccessLevel.ACTIVE_MEMBER, HttpMethod.DELETE, "/api/v2/ramen-logs/{logId:[0-9]+}"));
    }

}
