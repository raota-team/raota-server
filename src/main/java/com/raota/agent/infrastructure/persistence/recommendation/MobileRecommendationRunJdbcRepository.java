package com.raota.agent.infrastructure.persistence.recommendation;

import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.agent.domain.recommendation.model.RecommendationImpression;
import com.raota.agent.domain.recommendation.model.ShownRecommendation;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 노출 기록은 엔티티 없이 JDBC로 저장하고, 앱 식권(매장을 보여준 기록)만 다시 읽는다. */
@Repository
@RequiredArgsConstructor
public class MobileRecommendationRunJdbcRepository implements MobileRecommendationRunRepository {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public Long save(MobileRecommendationRun run) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tb_v2_recommendation_run (user_id, conditions, latitude, longitude, candidate_count,
                        algorithm_version, index_version, model_version, latency_ms, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, run.userId());
            statement.setString(2, run.conditionsJson());
            statement.setBigDecimal(3, run.latitude());
            statement.setBigDecimal(4, run.longitude());
            statement.setInt(5, run.candidateCount());
            statement.setString(6, run.algorithmVersion());
            statement.setString(7, run.indexVersion());
            statement.setString(8, run.modelVersion());
            statement.setInt(9, run.latencyMs());
            statement.setTimestamp(10, Timestamp.from(run.createdAt()));
            return statement;
        }, key);
        long runId = key.getKey().longValue();
        jdbc.batchUpdate(
                "INSERT INTO tb_v2_recommendation_item (run_id, item_rank, shop_id, score, reason) VALUES (?, ?, ?, ?, ?)",
                run.items(), run.items().size(), (statement, item) -> {
                    statement.setLong(1, runId);
                    statement.setInt(2, item.rank());
                    statement.setLong(3, item.shopId());
                    statement.setDouble(4, item.score());
                    statement.setString(5, item.reason());
                });
        return runId;
    }

    @Override
    public void deleteByUserId(Long userId) {
        jdbc.update("""
                DELETE item FROM tb_v2_recommendation_item item
                JOIN tb_v2_recommendation_run run ON run.id = item.run_id
                WHERE run.user_id = ?
                """, userId);
        jdbc.update("DELETE FROM tb_v2_recommendation_run WHERE user_id = ?", userId);
    }

    @Override
    public int countShownSince(Long userId, Instant since) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM tb_v2_recommendation_run run
                WHERE run.user_id = ? AND run.created_at >= ?
                  AND EXISTS (SELECT 1 FROM tb_v2_recommendation_item item WHERE item.run_id = run.id)
                """, Integer.class, userId, Timestamp.from(since));
        if (count == null) {
            return 0;
        }
        return count;
    }

    @Override
    public List<ShownRecommendation> findShownSince(Long userId, Instant since) {
        Map<Long, ShownRecommendation> runs = new LinkedHashMap<>();
        jdbc.query("""
                SELECT run.id, run.conditions, run.latitude, run.created_at,
                    item.item_rank, item.shop_id, item.score, item.reason
                FROM tb_v2_recommendation_run run
                JOIN tb_v2_recommendation_item item ON item.run_id = run.id
                WHERE run.user_id = ? AND run.created_at >= ?
                ORDER BY run.created_at DESC, run.id DESC, item.item_rank
                """, row -> {
            long runId = row.getLong("id");
            ShownRecommendation shown = runs.get(runId);
            if (shown == null) {
                shown = new ShownRecommendation(runId, row.getString("conditions"), row.getObject("latitude") != null,
                        row.getTimestamp("created_at").toInstant(), new ArrayList<>());
                runs.put(runId, shown);
            }
            shown.items()
                .add(new MobileRecommendationRun.Item(row.getInt("item_rank"), row.getLong("shop_id"),
                        row.getDouble("score"), row.getString("reason")));
        }, userId, Timestamp.from(since));
        return List.copyOf(runs.values());
    }

    @Override
    public boolean markOpened(Long userId, Long runId, Long shopId, Instant openedAt) {
        int updated = jdbc.update("""
                UPDATE tb_v2_recommendation_item item
                JOIN tb_v2_recommendation_run run ON run.id = item.run_id
                SET item.opened_at = ?
                WHERE run.id = ? AND run.user_id = ? AND item.shop_id = ? AND item.opened_at IS NULL
                """, Timestamp.from(openedAt), runId, userId, shopId);
        if (updated > 0) {
            return true;
        }
        Integer existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM tb_v2_recommendation_item item
                JOIN tb_v2_recommendation_run run ON run.id = item.run_id
                WHERE run.id = ? AND run.user_id = ? AND item.shop_id = ?
                """, Integer.class, runId, userId, shopId);
        return existing != null && existing > 0;
    }

    @Override
    public List<RecommendationImpression> findImpressions(Instant from, Instant to) {
        return jdbc.query("""
                SELECT run.id, run.user_id, run.created_at, run.algorithm_version, run.conditions,
                    item.item_rank, item.shop_id, item.opened_at
                FROM tb_v2_recommendation_run run
                JOIN tb_v2_recommendation_item item ON item.run_id = run.id
                WHERE run.created_at >= ? AND run.created_at < ?
                ORDER BY run.id, item.item_rank
                """, (row, index) -> {
            Timestamp opened = row.getTimestamp("opened_at");
            Instant openedAt = null;
            if (opened != null) {
                openedAt = opened.toInstant();
            }
            return new RecommendationImpression(row.getLong("id"), row.getLong("user_id"),
                    row.getTimestamp("created_at").toInstant(), row.getString("algorithm_version"),
                    row.getString("conditions"), row.getInt("item_rank"), row.getLong("shop_id"), openedAt);
        }, Timestamp.from(from), Timestamp.from(to));
    }

}
