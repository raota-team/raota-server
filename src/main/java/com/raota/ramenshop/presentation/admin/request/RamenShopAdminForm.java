package com.raota.ramenshop.presentation.admin.request;

import com.raota.ramenshop.domain.model.Address;
import com.raota.ramenshop.domain.model.BusinessHours;
import com.raota.ramenshop.domain.model.EventMenu;
import com.raota.ramenshop.domain.model.NormalMenu;
import com.raota.ramenshop.domain.model.RamenShop;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class RamenShopAdminForm {

    private Long id;

    @NotBlank(message = "가게 이름은 필수입니다.")
    private String name;

    private String branchName;

    private String naverMapId;

    @NotBlank(message = "도시는 필수입니다.")
    private String city;

    private String district;

    @NotBlank(message = "도로명 주소는 필수입니다.")
    private String street;

    private String detail;

    private java.math.BigDecimal latitude;

    private java.math.BigDecimal longitude;

    private String closedDays;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime openTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime closeTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime breakStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime breakEnd;

    private String instagramUrl;

    private String catchTableUrl;

    private String description;

    private String detailedDescription;

    private String parkingInfo;

    private String imageUrl;
    private String currentImageUrl;

    private String tags;

    private Boolean published = true;

    @Valid
    private List<NormalMenuForm> normalMenus = new ArrayList<>();

    @Valid
    private List<EventMenuForm> eventMenus = new ArrayList<>();

    public static RamenShopAdminForm from(RamenShop ramenShop) {
        RamenShopAdminForm form = new RamenShopAdminForm();
        form.setId(ramenShop.getId());
        form.setName(ramenShop.getName());
        form.setBranchName(ramenShop.getBranchName());
        form.setNaverMapId(ramenShop.getNaverMapId());
        if (ramenShop.getAddress() != null) {
            form.setCity(ramenShop.getAddress().city());
            form.setDistrict(ramenShop.getAddress().district());
            form.setStreet(ramenShop.getAddress().street());
            form.setDetail(ramenShop.getAddress().detail());
            form.setLatitude(ramenShop.getAddress().latitude());
            form.setLongitude(ramenShop.getAddress().longitude());
        }
        // ... rest of the method
        if (ramenShop.getBusinessHours() != null) {
            form.setClosedDays(ramenShop.getBusinessHours().closedDays());
            form.setOpenTime(ramenShop.getBusinessHours().openTime());
            form.setCloseTime(ramenShop.getBusinessHours().closeTime());
            form.setBreakStart(ramenShop.getBusinessHours().breakStart());
            form.setBreakEnd(ramenShop.getBusinessHours().breakEnd());
            form.setParkingInfo(ramenShop.getBusinessHours().parkingInfo());
        }
        form.setInstagramUrl(ramenShop.getInstagramUrl());
        form.setCatchTableUrl(ramenShop.getCatchTableUrl());
        form.setDescription(ramenShop.getDescription());
        form.setDetailedDescription(ramenShop.getDetailedDescription());
        form.setImageUrl(ramenShop.getImageUrl());
        form.setPublished(ramenShop.isPublished());
        form.setTags(String.join(", ", ramenShop.getTags() == null ? List.of() : ramenShop.getTags()));
        form.setNormalMenus(new ArrayList<>(
                (ramenShop.getNormalMenus() == null ? List.<NormalMenu>of() : ramenShop.getNormalMenus().getValues()).stream()
                        .map(NormalMenuForm::from)
                        .toList()
        ));
        form.setEventMenus(new ArrayList<>(
                (ramenShop.getEventMenus() == null ? List.<EventMenu>of() : ramenShop.getEventMenus().getValues()).stream()
                        .map(EventMenuForm::from)
                        .toList()
        ));
        return form;
    }

    public Address toAddress() {
        return Address.of(
                required(city),
                nullable(district),
                required(street),
                nullable(detail),
                latitude,
                longitude
        );
    }

    public BusinessHours toBusinessHours() {
        if (nullable(closedDays) == null && openTime == null && closeTime == null
                && breakStart == null && breakEnd == null && nullable(parkingInfo) == null) {
            return null;
        }
        return BusinessHours.of(nullable(closedDays), openTime, closeTime, breakStart, breakEnd, nullable(parkingInfo));
    }

    public List<String> toTags() {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public List<NormalMenu> toNormalMenus() {
        return normalMenus == null ? List.of() : normalMenus.stream()
                .filter(menu -> !menu.isEmpty())
                .map(NormalMenuForm::toEntity)
                .toList();
    }

    public List<EventMenu> toEventMenus() {
        return eventMenus == null ? List.of() : eventMenus.stream()
                .filter(menu -> !menu.isEmpty())
                .map(EventMenuForm::toEntity)
                .toList();
    }

    public boolean isPublishedValue() {
        return published == null || published;
    }

    private String required(String value) {
        String trimmed = nullable(value);
        if (trimmed == null) {
            throw new IllegalArgumentException("필수 입력값이 비어 있습니다.");
        }
        return trimmed;
    }

    private static String nullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    @Getter
    @Setter
    public static class NormalMenuForm {
        private String name;
        private Integer price;
        private boolean signature;
        private String imageUrl;
        private String currentImageUrl;

        public static NormalMenuForm from(NormalMenu menu) {
            NormalMenuForm form = new NormalMenuForm();
            form.setName(menu.getName());
            form.setPrice(menu.getPrice());
            form.setSignature(Boolean.TRUE.equals(menu.getIsSignature()));
            form.setImageUrl(menu.getImageUrl());
            form.setCurrentImageUrl(menu.getImageUrl());
            return form;
        }

        public boolean isEmpty() {
            return nullable(name) == null && price == null && nullable(imageUrl) == null && !signature;
        }

        public NormalMenu toEntity() {
            if (nullable(name) == null || price == null) {
                throw new IllegalArgumentException("일반 메뉴는 이름과 가격을 모두 입력해야 합니다.");
            }
            return NormalMenu.builder()
                    .name(nullable(name))
                    .price(price)
                    .isSignature(signature)
                    .imageUrl(nullable(imageUrl))
                    .build();
        }
    }

    @Getter
    @Setter
    public static class EventMenuForm {
        private String name;
        private String description;
        private Integer price;
        private String badgeText;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;

        private String imageUrl;
        private String currentImageUrl;

        public static EventMenuForm from(EventMenu menu) {
            EventMenuForm form = new EventMenuForm();
            form.setName(menu.getName());
            form.setDescription(menu.getDescription());
            form.setPrice(menu.getPrice());
            form.setBadgeText(menu.getBadgeText());
            form.setStartDate(menu.getStartDate());
            form.setEndDate(menu.getEndDate());
            form.setImageUrl(menu.getImageUrl());
            form.setCurrentImageUrl(menu.getImageUrl());
            return form;
        }

        public boolean isEmpty() {
            return nullable(name) == null
                    && nullable(description) == null
                    && price == null
                    && nullable(badgeText) == null
                    && startDate == null
                    && endDate == null
                    && nullable(imageUrl) == null;
        }

        public EventMenu toEntity() {
            if (nullable(name) == null || price == null) {
                throw new IllegalArgumentException("이벤트 메뉴는 이름과 가격을 모두 입력해야 합니다.");
            }
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("이벤트 메뉴 종료일은 시작일보다 빠를 수 없습니다.");
            }
            return EventMenu.builder()
                    .name(nullable(name))
                    .description(nullable(description))
                    .price(price)
                    .badgeText(nullable(badgeText))
                    .startDate(startDate)
                    .endDate(endDate)
                    .imageUrl(nullable(imageUrl))
                    .build();
        }
    }
}
