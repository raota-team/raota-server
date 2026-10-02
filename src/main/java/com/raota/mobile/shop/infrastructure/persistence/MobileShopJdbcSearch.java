package com.raota.mobile.shop.infrastructure.persistence;

import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.port.MobileShopSearchPort;
import com.raota.mobile.shop.application.query.MobileShopSort;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** JSON 조건과 Haversine 거리 정렬을 MySQL에서 계산한다. */
@Repository
@RequiredArgsConstructor
public class MobileShopJdbcSearch implements MobileShopSearchPort {

    private static final String HAVERSINE = """
            ROUND(6371000 * 2 * ASIN(LEAST(1, SQRT(
                POW(SIN(RADIANS(s.latitude - :latitude) / 2), 2)
                + COS(RADIANS(:latitude)) * COS(RADIANS(s.latitude))
                * POW(SIN(RADIANS(s.longitude - :longitude) / 2), 2)
            ))), 6)
            """;

    private static final Pattern DISTANCE_VALUE = Pattern.compile("[0-9]{1,8}(\\.[0-9]{1,6})?");

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<RankedShop> search(MobileShopSort sort, String query, String region, String ramenType,
            BigDecimal latitude, BigDecimal longitude, Cursor cursor, Integer limit) {
        String distance = latitude == null ? "NULL" : HAVERSINE;
        String sortValue = switch (sort) {
            case POPULAR -> "s.view_count";
            case LATEST -> "s.created_at";
            case DISTANCE -> "COALESCE(" + HAVERSINE + ", 99999999.000000)";
        };
        StringBuilder sql = new StringBuilder("SELECT ranked.* FROM (SELECT s.id, ").append(sortValue)
            .append(" AS sort_value, ")
            .append(distance)
            .append(" AS distance_value FROM tb_v2_shop s WHERE s.is_published = TRUE AND s.deleted_at IS NULL");
        var params = new MapSqlParameterSource();
        if (latitude != null) {
            params.addValue("latitude", latitude).addValue("longitude", longitude);
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (s.name LIKE :query OR s.branch_name LIKE :query)");
            params.addValue("query", "%" + query + "%");
        }
        if (region != null && !region.isBlank()) {
            sql.append(" AND s.region = :region");
            params.addValue("region", region);
        }
        if (ramenType != null && !ramenType.isBlank()) {
            sql.append(" AND JSON_CONTAINS(s.ramen_types, JSON_QUOTE(:ramenType))");
            params.addValue("ramenType", ramenType);
        }
        sql.append(") ranked WHERE 1 = 1");
        boolean ascending = sort == MobileShopSort.DISTANCE;
        if (cursor != null) {
            String operator = ascending ? ">" : "<";
            sql.append(" AND (ranked.sort_value ")
                .append(operator)
                .append(" :position OR (ranked.sort_value = :position AND ranked.id ")
                .append(operator)
                .append(" :cursorId))");
            params.addValue("position", position(sort, cursor)).addValue("cursorId", cursor.id());
        }
        sql.append(" ORDER BY ranked.sort_value ")
            .append(ascending ? "ASC" : "DESC")
            .append(", ranked.id ")
            .append(ascending ? "ASC" : "DESC");
        if (limit != null) {
            sql.append(" LIMIT :limit");
            params.addValue("limit", limit);
        }
        return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
            long id = rs.getLong("id");
            Cursor position = switch (sort) {
                case POPULAR -> Cursor.of(rs.getLong("sort_value"), id);
                case LATEST -> Cursor.of(rs.getTimestamp("sort_value").toInstant(), id);
                case DISTANCE -> new Cursor(rs.getBigDecimal("sort_value").toPlainString(), id);
            };
            return new RankedShop(id, position, rs.getBigDecimal("distance_value"));
        });
    }

    /** 거리 커서는 이 클래스가 만든 소수점 6자리 미터 값만 받는다. 지수 표기는 JDBC 변환에서 메모리를 소모한다. */
    private BigDecimal distance(String value) {
        if (!DISTANCE_VALUE.matcher(value).matches()) {
            throw new NumberFormatException(value);
        }
        return new BigDecimal(value);
    }

    private Object position(MobileShopSort sort, Cursor cursor) {
        try {
            return switch (sort) {
                case POPULAR -> Long.parseLong(cursor.sortValue());
                case LATEST -> Timestamp.from(Instant.parse(cursor.sortValue()));
                case DISTANCE -> distance(cursor.sortValue());
            };
        }
        catch (NumberFormatException | DateTimeParseException exception) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "커서가 올바르지 않습니다.");
        }
    }

}
