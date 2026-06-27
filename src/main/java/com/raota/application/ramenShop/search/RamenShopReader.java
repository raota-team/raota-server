package com.raota.application.ramenShop.search;

import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RamenShopReader {

    private final RamenShopRepository ramenShopRepository;

    public RamenShopReader(RamenShopRepository ramenShopRepository) {
        this.ramenShopRepository = ramenShopRepository;
    }

    public RamenShop getRamenShop(Long shopId) {
        return ramenShopRepository.findByIdAndPublishedTrue(shopId)
                .orElseThrow(() -> new IllegalArgumentException("라멘샵을 찾을 수 없습니다. id=" + shopId));
    }

    public List<RamenShop> getRamenShops(List<Long> shopIds) {
        return shopIds.stream()
                .map(this::getRamenShop)
                .toList();
    }

    public String normalizeText(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim();
    }

    public boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String primaryTag(RamenShop shop) {
        if (shop.getTags() == null || shop.getTags().isEmpty()) {
            return "";
        }
        return shop.getTags().getFirst();
    }

    public String addressText(RamenShop shop) {
        if (shop.getAddress() == null) {
            return "";
        }
        return shop.getAddress().fullAddress();
    }

    public String addressTextOrDefault(RamenShop shop) {
        String address = addressText(shop);
        return hasText(address) ? address : "주소 정보 없음";
    }

    public String tagsTextOrDefault(RamenShop shop) {
        if (shop.getTags() == null || shop.getTags().isEmpty()) {
            return "태그 정보 없음";
        }
        return String.join(", ", shop.getTags());
    }

    public String descriptionTextOrDefault(RamenShop shop) {
        if (hasText(shop.getDetailedDescription())) {
            return shop.getDetailedDescription();
        }
        if (!hasText(shop.getDescription())) {
            return "설명 정보 없음";
        }
        return shop.getDescription();
    }

    public String buildShopInfoContext(RamenShop shop) {
        return """
            매장명: %s
            주소: %s
            태그: %s
            설명: %s
            """.formatted(
                shop.getName(),
                addressTextOrDefault(shop),
                tagsTextOrDefault(shop),
                descriptionTextOrDefault(shop)
        );
    }
}
