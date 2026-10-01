package com.raota.mobile.shop.infrastructure.persistence;

import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.port.MobileShopBookmarkPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 복합 PK가 중복 쓰기를 막고 변경된 행에 대해서만 매장 카운터를 갱신한다. */
@Repository
@RequiredArgsConstructor
public class MobileShopJdbcBookmarks implements MobileShopBookmarkPort {

    private final JdbcTemplate jdbc;

    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public void add(Long userId, Long shopId, Instant createdAt) {
        int inserted = jdbc.update("""
                INSERT IGNORE INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, ?)
                """, userId, shopId, Timestamp.from(createdAt));
        if (inserted == 1) {
            jdbc.update("UPDATE tb_v2_shop SET bookmark_count = bookmark_count + 1 WHERE id = ?", shopId);
        }
    }

    @Override
    public void remove(Long userId, Long shopId) {
        int deleted = jdbc.update("DELETE FROM tb_v2_shop_bookmark WHERE user_id = ? AND shop_id = ?", userId, shopId);
        if (deleted == 1) {
            jdbc.update("UPDATE tb_v2_shop SET bookmark_count = GREATEST(bookmark_count - 1, 0) WHERE id = ?", shopId);
        }
    }

    @Override
    public Set<Long> bookmarkedShopIds(Long userId, List<Long> shopIds) {
        if (userId == null || shopIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(namedJdbc.queryForList("""
                SELECT shop_id FROM tb_v2_shop_bookmark WHERE user_id = :userId AND shop_id IN (:shopIds)
                """, Map.of("userId", userId, "shopIds", shopIds), Long.class));
    }

    @Override
    public List<SavedShop> saved(Long userId, Cursor cursor, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT bookmark.shop_id, bookmark.created_at
                FROM tb_v2_shop_bookmark bookmark
                JOIN tb_v2_shop shop ON shop.id = bookmark.shop_id
                WHERE bookmark.user_id = ? AND shop.is_published = TRUE AND shop.deleted_at IS NULL
                """);
        Instant at = null;
        if (cursor != null) {
            try {
                at = Instant.parse(cursor.sortValue());
            }
            catch (DateTimeParseException exception) {
                throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "커서가 올바르지 않습니다.");
            }
            sql.append(" AND (bookmark.created_at < ? OR (bookmark.created_at = ? AND bookmark.shop_id < ?))");
        }
        sql.append(" ORDER BY bookmark.created_at DESC, bookmark.shop_id DESC LIMIT ?");
        Object[] args = at == null ? new Object[] { userId, limit }
                : new Object[] { userId, Timestamp.from(at), Timestamp.from(at), cursor.id(), limit };
        return jdbc.query(sql.toString(), (rs, index) -> new SavedShop(rs.getLong("shop_id"),
                Cursor.of(rs.getTimestamp("created_at").toInstant(), rs.getLong("shop_id"))), args);
    }

}
