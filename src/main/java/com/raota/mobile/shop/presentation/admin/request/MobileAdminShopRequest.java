package com.raota.mobile.shop.presentation.admin.request;

import com.raota.mobile.shop.application.command.MobileAdminShopCommand;
import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import com.raota.mobile.shop.domain.model.MobileShopProfile;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** 빈 문자열은 값 없음으로 저장한다. 라멘 종류·태그는 공백을 다듬고 중복을 뺀다. */
public record MobileAdminShopRequest(@NotBlank @Size(max = 1000) String name, @Size(max = 255) String branchName,
        @NotBlank String address, @Size(max = 1000) String region,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude, @Size(max = 100) String phone, String tagline,
        String description, @NotNull @Size(max = 20) List<@NotNull @Size(max = 50) String> ramenTypes,
        @NotNull @Size(max = 30) List<@NotNull @Size(max = 50) String> tags,
        @Size(max = 1000) @Pattern(regexp = URL) String instagramUrl,
        @Size(max = 1000) @Pattern(regexp = URL) String reservationUrl,
        @Size(max = 1000) @Pattern(regexp = URL) String websiteUrl, @Size(max = 100) String naverPlaceId,
        @Size(max = 100) String kakaoPlaceId, @PositiveOrZero Integer priceMin, @PositiveOrZero Integer priceMax,
        @NotNull MobileShopBusinessStatus businessStatus, @Size(max = 50) String closedDaysText,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 1000) @Pattern(regexp = URL) String> imageUrls) {

    /** 비어 있거나 http(s) URL이다. */
    static final String URL = "^$|^https?://\\S+$";

    public MobileAdminShopCommand toCommand() {
        return new MobileAdminShopCommand(new MobileShopProfile(name.strip(), text(branchName), address.strip(),
                text(region), coordinate(latitude), coordinate(longitude), text(phone), text(tagline),
                text(description), labels(ramenTypes), labels(tags), text(instagramUrl), text(reservationUrl),
                text(websiteUrl), text(naverPlaceId), text(kakaoPlaceId), priceMin, priceMax, businessStatus,
                text(closedDaysText)), imageUrls.stream().map(String::strip).toList());
    }

    private static String text(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static List<String> labels(List<String> values) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String label = text(value);
            if (label != null) {
                unique.add(label);
            }
        }
        return new ArrayList<>(unique);
    }

    /** DB 컬럼 소수 자릿수(8)에 맞춘다. */
    private static BigDecimal coordinate(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(8, RoundingMode.HALF_UP);
    }

}
