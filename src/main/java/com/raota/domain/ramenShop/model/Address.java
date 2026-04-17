package com.raota.domain.ramenShop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record Address(
        @Column(name = "city") String city,
        @Column(name = "district") String district,
        @Column(name = "street") String street,
        @Column(name = "detail") String detail,
        @Column(name = "latitude") BigDecimal latitude,
        @Column(name = "longitude") BigDecimal longitude
) {
    public Address {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("도시는 필수입니다.");
        }
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("도로명은 필수입니다.");
        }
    }

    public static Address of(String city, String district, String street, String detail) {
        return new Address(city, district, street, detail, null, null);
    }

    public static Address of(String city, String district, String street, String detail, BigDecimal latitude, BigDecimal longitude) {
        return new Address(city, district, street, detail, latitude, longitude);
    }

    public String fullAddress() {
        return String.format("%s %s %s %s", city, district, street, detail);
    }

    public String simpleAddress() {
        return String.format("%s %s", city, district);
    }
}
