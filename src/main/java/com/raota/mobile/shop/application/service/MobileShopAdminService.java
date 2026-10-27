package com.raota.mobile.shop.application.service;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.common.page.OffsetPage;
import com.raota.mobile.shop.application.command.MobileAdminBusinessHourCommand;
import com.raota.mobile.shop.application.command.MobileAdminServicePerkCommand;
import com.raota.mobile.shop.application.command.MobileAdminShopCommand;
import com.raota.mobile.shop.application.result.MobileAdminShopDetail;
import com.raota.mobile.shop.application.result.MobileAdminShopRow;
import com.raota.mobile.shop.application.result.MobileShopBusinessHourResult;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import com.raota.mobile.shop.domain.model.MobileShopImage;
import com.raota.mobile.shop.domain.model.MobileShopProfile;
import com.raota.mobile.shop.domain.model.MobileShopServicePerk;
import com.raota.mobile.shop.domain.model.ServicePerkStatus;
import com.raota.mobile.shop.domain.model.ServicePerkType;
import com.raota.mobile.shop.domain.model.ShopImageSource;
import com.raota.mobile.shop.domain.repository.MobileShopBusinessHourRepository;
import com.raota.mobile.shop.domain.repository.MobileShopImageRepository;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import com.raota.mobile.shop.domain.repository.MobileShopServicePerkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 웹의 v2 매장 편집이다. 숨김 매장도 다루고 삭제는 하지 않는다(숨김으로 대신). 영업시간·혜택·사진은 요청 목록으로 통째로 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class MobileShopAdminService {

    private final MobileShopRepository shops;

    private final MobileShopImageRepository images;

    private final MobileShopBusinessHourRepository hours;

    private final MobileShopServicePerkRepository perks;

    private final Clock clock;

    @Transactional(readOnly = true)
    public OffsetPage<MobileAdminShopRow> list(String keyword, Boolean published, int page, int size) {
        var found = shops.findLiveForAdmin(
                blankToNull(keyword), published, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        List<Long> ids = found.getContent().stream().map(MobileShop::getId).toList();
        Map<Long, String> thumbnails = new HashMap<>();
        for (MobileShopImage image : images.findByShopIdInOrderBySortOrderAscIdAsc(ids)) {
            thumbnails.putIfAbsent(image.getShopId(), image.getUrl());
        }
        return OffsetPage.of(found, shop -> row(shop, thumbnails.get(shop.getId())));
    }

    @Transactional(readOnly = true)
    public MobileAdminShopDetail detail(Long shopId) {
        return detail(live(shopId));
    }

    @Transactional
    public MobileAdminShopDetail create(MobileAdminShopCommand command) {
        validate(command.profile());
        MobileShop shop = shops.saveAndFlush(MobileShop.create(command.profile(), now()));
        saveImages(shop.getId(), command.imageUrls(), Map.of());
        return detail(shop);
    }

    @Transactional
    public MobileAdminShopDetail update(Long shopId, MobileAdminShopCommand command) {
        validate(command.profile());
        MobileShop shop = liveForUpdate(shopId);
        shop.update(command.profile(), now());
        Map<String, ShopImageSource> sources = new HashMap<>();
        for (MobileShopImage image : images.findByShopIdInOrderBySortOrderAscIdAsc(List.of(shopId))) {
            sources.putIfAbsent(image.getUrl(), image.getSource());
        }
        images.deleteByShopId(shopId);
        saveImages(shopId, command.imageUrls(), sources);
        return detail(shop);
    }

    /** 저장하면 영업시간을 지금 확인한 것으로 기록한다. 빈 목록이면 영업시간 정보를 지운다. */
    @Transactional
    public MobileAdminShopDetail replaceBusinessHours(Long shopId, List<MobileAdminBusinessHourCommand> commands) {
        validateHours(commands);
        MobileShop shop = liveForUpdate(shopId);
        hours.deleteByShopId(shopId);
        hours.saveAll(commands.stream()
                .map(hour -> MobileShopBusinessHour.of(
                        shopId,
                        hour.dayOfWeek(),
                        hour.closed(),
                        hour.opensAt(),
                        hour.closesAt(),
                        hour.breakStart(),
                        hour.breakEnd(),
                        hour.lastOrderAt()))
                .toList());
        shop.markHoursVerified(now());
        return detail(shop);
    }

    @Transactional
    public MobileAdminShopDetail replaceServicePerks(Long shopId, List<MobileAdminServicePerkCommand> commands) {
        validatePerks(commands);
        MobileShop shop = liveForUpdate(shopId);
        Instant now = now();
        perks.deleteByShopId(shopId);
        perks.saveAll(commands.stream()
                .map(perk -> MobileShopServicePerk.of(
                        shopId, perk.type(), perk.status(), perk.price(), perk.condition(), now))
                .toList());
        return detail(shop);
    }

    @Transactional
    public MobileAdminShopDetail changeVisibility(Long shopId, boolean published) {
        MobileShop shop = live(shopId);
        shop.changeVisibility(published, now());
        return detail(shop);
    }

    private void saveImages(Long shopId, List<String> urls, Map<String, ShopImageSource> previousSources) {
        for (int index = 0; index < urls.size(); index++) {
            String url = urls.get(index);
            images.save(
                    MobileShopImage.of(shopId, url, previousSources.getOrDefault(url, ShopImageSource.ADMIN), index));
        }
    }

    private void validate(MobileShopProfile profile) {
        if ((profile.latitude() == null) != (profile.longitude() == null)) {
            throw invalid("위도와 경도는 함께 입력해야 합니다.");
        }
        if (profile.priceMin() != null && profile.priceMax() != null && profile.priceMin() > profile.priceMax()) {
            throw invalid("최저 가격이 최고 가격보다 클 수 없습니다.");
        }
    }

    private void validateHours(List<MobileAdminBusinessHourCommand> commands) {
        Set<Integer> days = new HashSet<>();
        for (MobileAdminBusinessHourCommand hour : commands) {
            if (!days.add(hour.dayOfWeek())) {
                throw invalid("같은 요일을 두 번 넣을 수 없습니다: " + hour.dayOfWeek());
            }
            if (hour.closed()) {
                if (hour.opensAt() != null
                        || hour.closesAt() != null
                        || hour.breakStart() != null
                        || hour.breakEnd() != null
                        || hour.lastOrderAt() != null) {
                    throw invalid("휴무일에는 시간을 넣지 않습니다: " + hour.dayOfWeek());
                }
                continue;
            }
            if (hour.opensAt() == null || hour.closesAt() == null) {
                throw invalid("영업일에는 시작·마감 시간이 필요합니다: " + hour.dayOfWeek());
            }
            if ((hour.breakStart() == null) != (hour.breakEnd() == null)) {
                throw invalid("브레이크 시작·끝은 함께 입력해야 합니다: " + hour.dayOfWeek());
            }
        }
    }

    private void validatePerks(List<MobileAdminServicePerkCommand> commands) {
        Set<ServicePerkType> types = EnumSet.noneOf(ServicePerkType.class);
        for (MobileAdminServicePerkCommand perk : commands) {
            if (!types.add(perk.type())) {
                throw invalid("같은 혜택을 두 번 넣을 수 없습니다: " + perk.type());
            }
            if (perk.price() != null && perk.status() != ServicePerkStatus.PAID) {
                throw invalid("가격은 유료 혜택에만 넣습니다: " + perk.type());
            }
        }
    }

    private MobileShop live(Long shopId) {
        return shops.findByIdAndDeletedAtIsNull(shopId).orElseThrow(this::notFound);
    }

    private MobileShop liveForUpdate(Long shopId) {
        return shops.findLiveForUpdate(shopId).orElseThrow(this::notFound);
    }

    private MobileAdminShopDetail detail(MobileShop shop) {
        List<Long> ids = List.of(shop.getId());
        List<String> imageUrls = images.findByShopIdInOrderBySortOrderAscIdAsc(ids).stream()
                .map(MobileShopImage::getUrl)
                .toList();
        List<MobileAdminShopDetail.Hour> shopHours = hours.findByShopIdInOrderByDayOfWeekAsc(ids).stream()
                .map(hour -> new MobileAdminShopDetail.Hour(
                        hour.getDayOfWeek(),
                        hour.isClosed(),
                        MobileShopBusinessHourResult.clockTime(hour.getOpensAt()),
                        MobileShopBusinessHourResult.clockTime(hour.getClosesAt()),
                        MobileShopBusinessHourResult.clockTime(hour.getBreakStart()),
                        MobileShopBusinessHourResult.clockTime(hour.getBreakEnd()),
                        MobileShopBusinessHourResult.clockTime(hour.getLastOrderAt())))
                .toList();
        List<MobileAdminShopDetail.Perk> shopPerks = perks.findByShopIdOrderByIdAsc(shop.getId()).stream()
                .map(perk -> new MobileAdminShopDetail.Perk(
                        perk.getPerkType(),
                        perk.getStatus(),
                        perk.getPrice(),
                        perk.getConditionText(),
                        perk.getVerifiedAt()))
                .toList();
        return new MobileAdminShopDetail(
                String.valueOf(shop.getId()),
                shop.getName(),
                shop.getBranchName(),
                shop.getAddress(),
                shop.getRegion(),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getPhone(),
                shop.getTagline(),
                shop.getDescription(),
                shop.getRamenTypes(),
                shop.getTags(),
                shop.getInstagramUrl(),
                shop.getReservationUrl(),
                shop.getWebsiteUrl(),
                shop.getNaverPlaceId(),
                shop.getKakaoPlaceId(),
                shop.getPriceMin(),
                shop.getPriceMax(),
                shop.getBusinessStatus(),
                shop.getClosedDaysText(),
                imageUrls,
                shop.isPublished(),
                shop.getHoursVerifiedAt(),
                shopHours,
                shopPerks,
                shop.getAiReviewSummary(),
                shop.getAiSummaryKeywords(),
                shop.getAiSummaryGeneratedAt(),
                shop.getViewCount(),
                shop.getLogCount(),
                shop.getBookmarkCount(),
                shop.getCreatedAt(),
                shop.getUpdatedAt());
    }

    private static MobileAdminShopRow row(MobileShop shop, String thumbnailUrl) {
        return new MobileAdminShopRow(
                String.valueOf(shop.getId()),
                shop.getName(),
                shop.getBranchName(),
                shop.getAddress(),
                shop.getRegion(),
                shop.getBusinessStatus(),
                shop.isPublished(),
                thumbnailUrl,
                shop.getLogCount(),
                shop.getBookmarkCount(),
                shop.getHoursVerifiedAt(),
                shop.getUpdatedAt());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private MobileException notFound() {
        return new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다.");
    }

    private static MobileException invalid(String message) {
        return new MobileException(MobileErrorCode.VALIDATION_ERROR, message);
    }
}
