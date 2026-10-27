package com.raota.mobile.shop.application.query;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 매장 목록과 지도가 함께 쓰는 검색 조건이다. 문자열·좌표가 null이면 그 조건을 보지 않는다.
 *
 * <p>
 * 잘못된 조건으로는 만들 수 없다. 거리순이거나 좌표를 하나라도 주면 위도·경도가 둘 다 있어야 하고, 좌표는 범위를 확인한 뒤 저장 정밀도인
 * 소수점 8자리로 맞춘다. 지수 표기로 자릿수가 극단적으로 큰 값은 JDBC가 십진 문자열로 펼치며 메모리를 소모하므로 범위 비교 전에 거절한다.
 * </p>
 */
public record MobileShopSearchCondition(
        MobileShopSort sort,
        String query,
        String region,
        String ramenType,
        boolean openNow,
        BigDecimal latitude,
        BigDecimal longitude) {

    public MobileShopSearchCondition {
        if (sort == MobileShopSort.DISTANCE || latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                throw invalid();
            }
        }
        latitude = coordinate(latitude, 90);
        longitude = coordinate(longitude, 180);
    }

    private static BigDecimal coordinate(BigDecimal value, int limit) {
        if (value == null) {
            return null;
        }
        if (value.scale() > 20 || value.precision() > 30 || value.abs().compareTo(BigDecimal.valueOf(limit)) > 0) {
            throw invalid();
        }
        return value.setScale(8, RoundingMode.HALF_UP);
    }

    private static MobileException invalid() {
        return new MobileException(MobileErrorCode.VALIDATION_ERROR, "매장 목록 요청 값을 확인해 주세요.");
    }
}
