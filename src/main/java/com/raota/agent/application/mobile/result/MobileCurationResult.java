package com.raota.agent.application.mobile.result;

import com.raota.mobile.shop.application.result.MobileShopSummary;

/** 앱의 {@code TodayCuration}과 같은 형태다. */
public record MobileCurationResult(MobileShopSummary shop, String title, String reason) {
}
