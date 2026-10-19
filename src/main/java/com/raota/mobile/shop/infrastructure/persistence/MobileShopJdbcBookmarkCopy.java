package com.raota.mobile.shop.infrastructure.persistence;

import com.raota.mobile.shop.application.port.MobileShopBookmarkCopyPort;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * web 모듈을 쓰지 않고 v1 테이블을 직접 읽는다. 시각은 {@link LocalDateTime}으로 주고받아 드라이버·JVM 시간대 변환 없이 저장된 값
 * 그대로 다룬다.
 */
@Repository
@RequiredArgsConstructor
public class MobileShopJdbcBookmarkCopy implements MobileShopBookmarkCopyPort {

    private final JdbcTemplate jdbc;

    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public List<LegacyBookmark> legacyBookmarks() {
        return jdbc.query("""
                SELECT member_profile_id, ramen_shop_id, marking_at, is_deleted FROM tb_bookmark ORDER BY id
                """, (rs, index) -> new LegacyBookmark(rs.getLong("member_profile_id"), rs.getLong("ramen_shop_id"),
                rs.getObject("marking_at", LocalDateTime.class), rs.getBoolean("is_deleted")));
    }

    @Override
    public List<BookmarkKey> bookmarkKeys() {
        return jdbc.query("SELECT user_id, shop_id FROM tb_v2_shop_bookmark",
                (rs, index) -> new BookmarkKey(rs.getLong("user_id"), rs.getLong("shop_id")));
    }

    @Override
    public void insert(BookmarkKey key, LocalDateTime createdAt) {
        jdbc.update("INSERT INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, ?)", key.userId(),
                key.shopId(), createdAt);
    }

    @Override
    public void recountBookmarks(Collection<Long> shopIds) {
        if (shopIds.isEmpty()) {
            return;
        }
        namedJdbc.update("""
                UPDATE tb_v2_shop shop SET bookmark_count =
                    (SELECT COUNT(*) FROM tb_v2_shop_bookmark bookmark WHERE bookmark.shop_id = shop.id)
                WHERE shop.id IN (:ids)
                """, Map.of("ids", shopIds));
    }

}
