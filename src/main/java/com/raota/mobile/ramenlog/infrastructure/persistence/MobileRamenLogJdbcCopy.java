package com.raota.mobile.ramenlog.infrastructure.persistence;

import com.raota.mobile.ramenlog.application.port.MobileRamenLogCopyPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

/**
 * web 모듈을 쓰지 않고 v1 테이블을 직접 읽는다. 시각은 {@link LocalDateTime}으로 주고받아 드라이버·JVM 시간대 변환 없이 저장된 값
 * 그대로 다룬다.
 */
@Repository
@RequiredArgsConstructor
public class MobileRamenLogJdbcCopy implements MobileRamenLogCopyPort {

    private final JdbcTemplate jdbc;

    private final NamedParameterJdbcTemplate namedJdbc;

    private final ObjectMapper mapper;

    @Override
    public List<LegacyRamenLog> legacyLogs() {
        return jdbc.query("""
                SELECT id, ramen_shop_id, member_id, image_url, note, broth_notes, noodle_notes, seasoning_notes,
                    topping_notes, revisit, is_public, visited_at, created_at, updated_at, menu_name, ramen_type,
                    is_deleted
                FROM tb_ramen_log ORDER BY id
                """,
                (rs, index) -> new LegacyRamenLog(rs.getLong("id"), rs.getLong("ramen_shop_id"),
                        rs.getLong("member_id"), rs.getString("image_url"), rs.getString("note"),
                        rs.getString("broth_notes"), rs.getString("noodle_notes"), rs.getString("seasoning_notes"),
                        rs.getString("topping_notes"), rs.getString("revisit"), rs.getBoolean("is_public"),
                        rs.getObject("visited_at", LocalDate.class), time(rs, "created_at"), time(rs, "updated_at"),
                        rs.getString("menu_name"), rs.getString("ramen_type"), rs.getBoolean("is_deleted")));
    }

    @Override
    public List<LegacyLike> legacyLikes() {
        return jdbc.query("SELECT ramen_log_id, member_id, created_at FROM tb_ramen_log_like ORDER BY id", (rs,
                index) -> new LegacyLike(rs.getLong("ramen_log_id"), rs.getLong("member_id"), time(rs, "created_at")));
    }

    @Override
    public Map<Long, String> logIdempotencyKeys() {
        Map<Long, String> result = new HashMap<>();
        jdbc.query("SELECT id, idempotency_key FROM tb_v2_ramen_log", rs -> {
            result.put(rs.getLong("id"), rs.getString("idempotency_key"));
        });
        return result;
    }

    @Override
    public List<LikeKey> likeKeys() {
        return jdbc.query("SELECT user_id, ramen_log_id FROM tb_v2_ramen_log_like",
                (rs, index) -> new LikeKey(rs.getLong("user_id"), rs.getLong("ramen_log_id")));
    }

    @Override
    public void insertLog(NewRamenLog log) {
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    revisit_intention, note, taste_note_codes, visibility, like_count, comment_count, idempotency_key,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?)
                """, log.id(), log.userId(), log.shopId(), log.visitedAt(), log.menuName(), log.ramenType(),
                log.revisitIntention().name(), log.note(), mapper.writeValueAsString(log.tasteNoteCodes()),
                log.visibility().name(), log.idempotencyKey(), log.createdAt(), log.updatedAt());
    }

    @Override
    public void insertImage(Long logId, String url) {
        jdbc.update("INSERT INTO tb_v2_ramen_log_image (ramen_log_id, url, sort_order) VALUES (?, ?, 0)", logId, url);
    }

    @Override
    public void insertLike(LikeKey key, LocalDateTime createdAt) {
        jdbc.update("INSERT INTO tb_v2_ramen_log_like (user_id, ramen_log_id, created_at) VALUES (?, ?, ?)",
                key.userId(), key.logId(), createdAt);
    }

    @Override
    public Map<Long, Integer> liveLogCountsByUser(Collection<Long> userIds) {
        return liveLogCounts("user_id", userIds);
    }

    @Override
    public Map<Long, Integer> liveLogCountsByShop(Collection<Long> shopIds) {
        return liveLogCounts("shop_id", shopIds);
    }

    @Override
    public void recountLikes(Collection<Long> logIds) {
        if (logIds.isEmpty()) {
            return;
        }
        namedJdbc.update("""
                UPDATE tb_v2_ramen_log target SET like_count =
                    (SELECT COUNT(*) FROM tb_v2_ramen_log_like liked WHERE liked.ramen_log_id = target.id)
                WHERE target.id IN (:ids)
                """, Map.of("ids", logIds));
    }

    /** {@code column}은 이 클래스의 고정 값만 받는다. */
    private Map<Long, Integer> liveLogCounts(String column, Collection<Long> ids) {
        Map<Long, Integer> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        namedJdbc.query(
                "SELECT " + column + " AS owner_id, COUNT(*) AS logs FROM tb_v2_ramen_log "
                        + "WHERE deleted_at IS NULL AND " + column + " IN (:ids) GROUP BY " + column,
                Map.of("ids", ids), rs -> {
                    result.put(rs.getLong("owner_id"), rs.getInt("logs"));
                });
        return result;
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

}
