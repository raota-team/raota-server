package com.raota.mobile.notification.presentation;

import com.raota.global.security.AccessLevel;
import com.raota.global.security.AccessRule;
import com.raota.global.security.AccessRuleContributor;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** 해제는 로그아웃 때 부르므로 온보딩 중인 회원도 쓸 수 있게 AUTHENTICATED로 둔다. */
@Component
public class MobileNotificationAccessRules implements AccessRuleContributor {

    @Override
    public List<AccessRule> accessRules() {
        return List.of(
                new AccessRule(AccessLevel.ACTIVE_MEMBER, HttpMethod.POST, "/api/v2/members/me/push-tokens"),
                new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.DELETE, "/api/v2/members/me/push-tokens/{token}"));
    }
}
