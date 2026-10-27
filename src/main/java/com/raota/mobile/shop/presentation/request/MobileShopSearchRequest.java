package com.raota.mobile.shop.presentation.request;

import com.raota.mobile.shop.application.query.MobileShopSearchCondition;
import com.raota.mobile.shop.application.query.MobileShopSort;
import java.math.BigDecimal;
import org.springdoc.core.annotations.ParameterObject;

/**
 * 매장 목록·지도의 쿼리 파라미터다.
 *
 * <p>
 * {@code @ModelAttribute} 레코드 바인딩은 항목의 {@code @RequestParam(defaultValue)}를 보지 않으므로 기본값은 생성자에서 채운다.
 * 값이 없을 수 있는 항목은 감싼 타입으로 받는다.
 * </p>
 */
@ParameterObject
public record MobileShopSearchRequest(
        MobileShopSort sort,
        String query,
        String region,
        String ramenType,
        Boolean openNow,
        BigDecimal latitude,
        BigDecimal longitude) {

    public MobileShopSearchRequest {
        if (sort == null) {
            sort = MobileShopSort.POPULAR;
        }
        if (openNow == null) {
            openNow = false;
        }
    }

    public MobileShopSearchCondition toCondition() {
        return new MobileShopSearchCondition(sort, query, region, ramenType, openNow, latitude, longitude);
    }
}
