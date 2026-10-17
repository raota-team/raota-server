package com.raota.mobile.shop.application.result;

import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.modulith.NamedInterface;

/**
 * 앱 목록의 한 매장과 현재 회원의 가고 싶어요 상태다. AI 추천·큐레이션도 같은 앱 형태({@code ShopSummary})로 내보내므로 facade
 * 공개 API에 포함한다.
 */
@NamedInterface("facade")
public record MobileShopSummary(String id, String name, String branchName, String address, String region,
        BigDecimal latitude, BigDecimal longitude, String imageUrl, String tagline, List<String> ramenTypes,
        List<String> tags, int logCount, int bookmarkCount, boolean isBookmarked,
        MobileShopBusinessStatus businessStatus, Boolean isOpen, Integer distanceMeters) {
}
