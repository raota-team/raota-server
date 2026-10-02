package com.raota.mobile.shop.application.facade;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 매장 참조와 기록 수·점수 합계를 소유 모듈의 저장소를 통해 제공한다. */
@Service
@RequiredArgsConstructor
public class MobileShopRamenLogFacade {

    private final MobileShopRepository shops;

    public MobileShopRef findPublishedShopRef(Long shopId) {
        return shops.findByIdAndPublishedTrueAndDeletedAtIsNull(shopId)
            .map(this::ref)
            .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다."));
    }

    public Map<Long, MobileShopRef> findPublishedShopRefs(Collection<Long> shopIds) {
        if (shopIds.isEmpty()) {
            return Map.of();
        }
        return refs(shops.findByIdInAndPublishedTrueAndDeletedAtIsNull(shopIds));
    }

    /** 이미 작성된 기록은 매장의 공개 상태와 관계없이 해당 매장 이름을 표시한다. */
    public Map<Long, MobileShopRef> findShopRefs(Collection<Long> shopIds) {
        if (shopIds.isEmpty()) {
            return Map.of();
        }
        return refs(shops.findAllById(shopIds));
    }

    @Transactional
    public void recordLogAdded(Long shopId, Integer satisfaction) {
        shops.recordLogAdded(shopId, satisfaction == null ? 0 : satisfaction, satisfaction == null ? 0 : 1);
    }

    @Transactional
    public void recordLogRemoved(Long shopId, Integer satisfaction) {
        shops.recordLogRemoved(shopId, satisfaction == null ? 0 : satisfaction, satisfaction == null ? 0 : 1);
    }

    @Transactional
    public void recordSatisfactionChanged(Long shopId, Integer oldScore, Integer newScore) {
        if (Objects.equals(oldScore, newScore)) {
            return;
        }
        shops.recordSatisfactionChanged(shopId, (newScore == null ? 0 : newScore) - (oldScore == null ? 0 : oldScore),
                (newScore == null ? 0 : 1) - (oldScore == null ? 0 : 1));
    }

    private Map<Long, MobileShopRef> refs(List<MobileShop> found) {
        Map<Long, MobileShopRef> result = new HashMap<>(found.size());
        for (MobileShop shop : found) {
            result.put(shop.getId(), ref(shop));
        }
        return result;
    }

    private MobileShopRef ref(MobileShop shop) {
        return new MobileShopRef(shop.getId(), shop.getName(), shop.getBranchName(), shop.getRegion());
    }

    public record MobileShopRef(Long id, String name, String branchName, String region) {
    }

}
