package com.raota.mobile.shop.application.port;

import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.shop.application.query.MobileShopSort;
import java.math.BigDecimal;
import java.util.List;

/** SQL 정렬값과 ID를 함께 읽어 안정적인 다음 페이지를 만든다. */
public interface MobileShopSearchPort {

    List<RankedShop> search(MobileShopSort sort, String query, String region, String ramenType, BigDecimal latitude,
            BigDecimal longitude, Cursor cursor, Integer limit);

    record RankedShop(Long id, Cursor position, BigDecimal distance) {
    }

}
