package com.raota.mobile.account.presentation;

import com.raota.global.security.AccessLevel;
import com.raota.global.security.AccessRule;
import com.raota.global.security.AccessRuleContributor;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** v2 회원 API의 공개 여부와 인증 요구 사항을 등록한다. */
@Component
public class MobileAccessRules implements AccessRuleContributor {

    @Override
    public List<AccessRule> accessRules() {
        return List.of(new AccessRule(AccessLevel.PUBLIC, HttpMethod.POST, "/api/v2/auth/oauth/login"),
                new AccessRule(AccessLevel.PUBLIC, HttpMethod.POST, "/api/v2/auth/token/reissue"),
                new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.POST, "/api/v2/auth/logout"));
    }

}
