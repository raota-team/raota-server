package com.raota.web.ramenshop.presentation.response;

import com.raota.web.ramenshop.domain.model.EventMenu;
import com.raota.web.ramenshop.domain.model.NormalMenu;
import com.raota.web.ramenshop.domain.model.RamenShop;
import java.util.List;

public record RamenShopMenuOptionsResponse(Long shopId, String shopName, List<MenuOptionResponse> normalMenus,
        List<MenuOptionResponse> eventMenus) {

    public static RamenShopMenuOptionsResponse from(RamenShop ramenShop, List<NormalMenu> normalMenus,
            List<EventMenu> eventMenus) {
        return new RamenShopMenuOptionsResponse(ramenShop.getId(), ramenShop.getName(),
                normalMenus.stream().map(menu -> new MenuOptionResponse(menu.getId(), menu.getName())).toList(),
                eventMenus.stream().map(menu -> new MenuOptionResponse(menu.getId(), menu.getName())).toList());
    }

    public record MenuOptionResponse(Long id, String name) {
    }
}
