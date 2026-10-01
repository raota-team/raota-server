package com.raota.mobile.shop.presentation;

import com.raota.global.security.AccessLevel;
import com.raota.global.security.AccessRule;
import com.raota.global.security.AccessRuleContributor;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** 매장 API의 경로별 접근 등급을 매장 모듈 안에서 등록한다. */
@Component
public class MobileShopAccessRules implements AccessRuleContributor {

    @Override
    public List<AccessRule> accessRules() {
        return List.of(new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v2/shops"),
                new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v2/shops/map-pins"),
                new AccessRule(AccessLevel.PUBLIC, HttpMethod.GET, "/api/v2/shops/{shopId:[0-9]+}"),
                new AccessRule(AccessLevel.ACTIVE_MEMBER, HttpMethod.PUT, "/api/v2/shops/{shopId:[0-9]+}/bookmark"),
                new AccessRule(AccessLevel.ACTIVE_MEMBER, HttpMethod.DELETE, "/api/v2/shops/{shopId:[0-9]+}/bookmark"),
                new AccessRule(AccessLevel.AUTHENTICATED, HttpMethod.GET, "/api/v2/members/me/bookmarked-shops"));
    }

}
