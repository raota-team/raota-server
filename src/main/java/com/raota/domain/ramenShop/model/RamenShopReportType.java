package com.raota.domain.ramenShop.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RamenShopReportType {
    OPENING_HOURS_ERROR("영업시간 오류"),
    CLOSED("폐업"),
    MENU_INFO_ERROR("메뉴정보 오류"),
    OTHERS("기타");

    private final String description;
}
