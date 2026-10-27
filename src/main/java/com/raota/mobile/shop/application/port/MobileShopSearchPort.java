package com.raota.mobile.shop.application.port;

import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.shop.application.query.MobileShopSearchCondition;
import java.math.BigDecimal;
import java.util.List;

/** SQL 정렬값과 ID를 함께 읽어 안정적인 다음 페이지를 만든다. */
public interface MobileShopSearchPort {

    /**
     * 조건에 맞는 공개 매장을 정렬 순서대로 {@code limit}개까지(null이면 전부) 읽는다. {@code openNow}는 영업시간을 읽은 뒤 서비스가 거르므로 여기서는 보지
     * 않는다.
     */
    List<RankedShop> search(MobileShopSearchCondition condition, Cursor cursor, Integer limit);

    record RankedShop(Long id, Cursor position, BigDecimal distance) {}
}
