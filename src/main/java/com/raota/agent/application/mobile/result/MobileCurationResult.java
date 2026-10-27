package com.raota.agent.application.mobile.result;

import com.raota.mobile.shop.application.result.MobileShopSummaryResult;

/** 앱의 {@code TodayCuration}과 같은 형태다. */
public record MobileCurationResult(MobileShopSummaryResult shop, String title, String reason) {}
