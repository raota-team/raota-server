package com.raota.mobile.shop.application.result;

import java.math.BigDecimal;
import java.util.List;

/** 지도에 표시할 공개 매장의 최소 좌표 정보다. */
public record MobileShopMapPin(String id, String name, String branchName, BigDecimal latitude, BigDecimal longitude,
        List<String> ramenTypes, Boolean isOpen) {
}
