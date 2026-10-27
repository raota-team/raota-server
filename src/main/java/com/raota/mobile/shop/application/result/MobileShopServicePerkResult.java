package com.raota.mobile.shop.application.result;

import com.raota.mobile.shop.domain.model.MobileShopServicePerk;
import com.raota.mobile.shop.domain.model.ServicePerkStatus;
import com.raota.mobile.shop.domain.model.ServicePerkType;
import java.time.Instant;

/** 매장 서비스 종류별 제공 조건이다. */
public record MobileShopServicePerkResult(
        ServicePerkType type, ServicePerkStatus status, Integer price, String conditionText, Instant verifiedAt) {

    public static MobileShopServicePerkResult from(MobileShopServicePerk perk) {
        return new MobileShopServicePerkResult(
                perk.getPerkType(), perk.getStatus(), perk.getPrice(), perk.getConditionText(), perk.getVerifiedAt());
    }
}
