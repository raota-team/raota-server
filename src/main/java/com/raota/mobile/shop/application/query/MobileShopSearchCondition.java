package com.raota.mobile.shop.application.query;

import java.math.BigDecimal;

/** 매장 목록과 지도가 함께 쓰는 검색 조건이다. 문자열·좌표가 null이면 그 조건을 보지 않는다. */
public record MobileShopSearchCondition(
        MobileShopSort sort,
        String query,
        String region,
        String ramenType,
        boolean openNow,
        BigDecimal latitude,
        BigDecimal longitude) {}
