package com.raota.mobile.shop.application.command;

import com.raota.mobile.shop.domain.model.ServicePerkStatus;
import com.raota.mobile.shop.domain.model.ServicePerkType;

/** 가격은 유료(PAID)일 때만 둔다. */
public record MobileAdminServicePerkCommand(
        ServicePerkType type, ServicePerkStatus status, Integer price, String condition) {}
