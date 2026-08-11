package com.raota.agent.domain.retrieval.document.factory;

import com.raota.ramenshop.domain.model.Address;
import com.raota.ramenshop.domain.model.BusinessHours;
import com.raota.ramenshop.domain.model.EventMenu;
import com.raota.ramenshop.domain.model.NormalMenu;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.model.ShopStats;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentFactory;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentSource;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentType;
import com.raota.agent.domain.retrieval.document.RetrievalMetadataKeys;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class RamenShopProfileDocumentFactory implements RetrievalDocumentFactory<RamenShop> {

    @Override
    public List<Document> create(RamenShop shop) {
        String region = extractRegion(shop.getAddress());
        List<String> menuNames = extractMenuNames(shop);
        List<String> tags = shop.getTags() == null ? List.of() : List.copyOf(shop.getTags());

        String content = """
                %s는 %s에 위치한 라멘집이다.
                대표 메뉴는 %s이다.
                태그는 %s이다.
                영업 정보는 %s이다.
                방문 수는 %d회이고 북마크 수는 %d회이다.
                %s
                """.formatted(
                formatShopName(shop),
                region,
                joinOrFallback(menuNames, "대표 메뉴 정보 없음"),
                joinOrFallback(tags, "태그 정보 없음"),
                formatBusinessHours(shop.getBusinessHours()),
                visitCount(shop.getStats()),
                bookmarkCount(shop.getStats()),
                defaultText(descriptionText(shop), "가게 설명 정보는 아직 없다.")
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.SHOP_PROFILE.name());
        metadata.put(RetrievalMetadataKeys.SOURCE, RetrievalDocumentSource.RAMEN_SHOP.name());
        metadata.put(RetrievalMetadataKeys.SOURCE_ID, String.valueOf(shop.getId()));
        metadata.put(RetrievalMetadataKeys.SHOP_ID, String.valueOf(shop.getId()));
        metadata.put(RetrievalMetadataKeys.SHOP_NAME, formatShopName(shop));
        metadata.put(RetrievalMetadataKeys.REGION, region);
        metadata.put(RetrievalMetadataKeys.MENU_NAMES, menuNames);
        metadata.put(RetrievalMetadataKeys.TAGS, tags);

        return List.of(new Document(content, metadata));
    }

    private String formatShopName(RamenShop shop) {
        if (shop.getBranchName() == null || shop.getBranchName().isBlank()) {
            return shop.getName();
        }
        return shop.getName() + " " + shop.getBranchName();
    }

    private String extractRegion(Address address) {
        if (address == null) {
            return "위치 정보 없음";
        }
        return defaultText(address.simpleAddress(), "위치 정보 없음");
    }

    private String formatBusinessHours(BusinessHours businessHours) {
        if (businessHours == null) {
            return "영업시간 정보 없음";
        }
        return businessHours.toDisplayString();
    }

    private int visitCount(ShopStats stats) {
        return stats == null ? 0 : stats.visitCount();
    }

    private int bookmarkCount(ShopStats stats) {
        return stats == null ? 0 : stats.bookmarkCount();
    }

    private List<String> extractMenuNames(RamenShop shop) {
        List<String> menuNames = new ArrayList<>();

        if (shop.getNormalMenus() != null) {
            for (NormalMenu menu : shop.getNormalMenus().getValues()) {
                menuNames.add(menu.getName());
            }
        }

        if (shop.getEventMenus() != null) {
            for (EventMenu menu : shop.getEventMenus().getValues()) {
                menuNames.add(menu.getName());
            }
        }

        return List.copyOf(menuNames);
    }

    private String joinOrFallback(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return String.join(", ", values);
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String descriptionText(RamenShop shop) {
        if (shop.getDetailedDescription() != null && !shop.getDetailedDescription().isBlank()) {
            return shop.getDetailedDescription();
        }
        return shop.getDescription();
    }
}
