package com.raota.admin.ramenShop.service;

import com.raota.admin.ramenShop.request.RamenShopAdminForm;
import com.raota.admin.ramenShop.response.RamenShopAdminSummaryResponse;
import com.raota.domain.ramenShop.model.EventMenus;
import com.raota.domain.ramenShop.model.NormalMenus;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.global.cache.CacheInvalidationPublisher;
import com.raota.global.file.FileUploader;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RamenShopAdminService {

    private final RamenShopRepository ramenShopRepository;
    private final FileUploader fileUploader;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    @Transactional(readOnly = true)
    public List<RamenShopAdminSummaryResponse> getShopSummaries() {
        return ramenShopRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(shop -> new RamenShopAdminSummaryResponse(
                        shop.getId(),
                        shop.getName(),
                        shop.getAddress() == null ? "" : Stream.of(
                                        shop.getAddress().city(),
                                        shop.getAddress().district(),
                                        shop.getAddress().street(),
                                        shop.getAddress().detail()
                                )
                                .filter(value -> value != null && !value.isBlank())
                                .reduce((left, right) -> left + " " + right)
                                .orElse(""),
                        fileUploader.getAccessibleUrl(shop.getImageUrl())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public RamenShopAdminForm getForm(Long shopId) {
        RamenShop ramenShop = getShop(shopId);
        RamenShopAdminForm form = RamenShopAdminForm.from(ramenShop);
        form.setCurrentImageUrl(fileUploader.getAccessibleUrl(ramenShop.getImageUrl()));
        form.getNormalMenus().forEach(menu -> menu.setCurrentImageUrl(fileUploader.getAccessibleUrl(menu.getImageUrl())));
        form.getEventMenus().forEach(menu -> menu.setCurrentImageUrl(fileUploader.getAccessibleUrl(menu.getImageUrl())));
        return form;
    }

    @Transactional
    public Long createShop(RamenShopAdminForm form) {
        RamenShop ramenShop = RamenShop.builder()
                .name(form.getName().trim())
                .branchName(blankToNull(form.getBranchName()))
                .naverMapId(blankToNull(form.getNaverMapId()))
                .address(form.toAddress())
                .businessHours(form.toBusinessHours())
                .tags(form.toTags())
                .instagramUrl(blankToNull(form.getInstagramUrl()))
                .catchTableUrl(blankToNull(form.getCatchTableUrl()))
                .description(blankToNull(form.getDescription()))
                .imageUrl(blankToNull(form.getImageUrl()))
                .normalMenus(NormalMenus.init())
                .eventMenus(EventMenus.init())
                .build();

        ramenShop.replaceNormalMenus(form.toNormalMenus());
        ramenShop.replaceEventMenus(form.toEventMenus());

        Long shopId = ramenShopRepository.save(ramenShop).getId();
        cacheInvalidationPublisher.publishAll("ramenShopList");
        return shopId;
    }

    @Transactional
    public void updateShop(Long shopId, RamenShopAdminForm form) {
        RamenShop ramenShop = getShop(shopId);
        String nextImageUrl = blankToNull(form.getImageUrl());
        if (nextImageUrl != null
                && ramenShop.getImageUrl() != null
                && !ramenShop.getImageUrl().equals(nextImageUrl)) {
            fileUploader.delete(ramenShop.getImageUrl());
        }
        ramenShop.updateBasicInfo(
                form.getName().trim(),
                blankToNull(form.getBranchName()),
                blankToNull(form.getNaverMapId()),
                form.toAddress(),
                form.toBusinessHours(),
                form.toTags(),
                blankToNull(form.getInstagramUrl()),
                blankToNull(form.getCatchTableUrl()),
                blankToNull(form.getDescription()),
                nextImageUrl
        );
        ramenShop.replaceNormalMenus(form.toNormalMenus());
        ramenShop.replaceEventMenus(form.toEventMenus());
        cacheInvalidationPublisher.publish("ramenShopDetail", String.valueOf(shopId));
        cacheInvalidationPublisher.publishAll("ramenShopList");
    }

    @Transactional
    public void deleteShop(Long shopId) {
        RamenShop ramenShop = getShop(shopId);
        if (ramenShop.getImageUrl() != null) {
            fileUploader.delete(ramenShop.getImageUrl());
        }
        ramenShopRepository.delete(ramenShop);
        cacheInvalidationPublisher.publish("ramenShopDetail", String.valueOf(shopId));
        cacheInvalidationPublisher.publishAll("ramenShopList");
    }

    private RamenShop getShop(Long shopId) {
        return ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("없는 라멘가게 입니다."));
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
