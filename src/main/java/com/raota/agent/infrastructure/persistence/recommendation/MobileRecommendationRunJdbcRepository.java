package com.raota.agent.infrastructure.persistence.recommendation;

import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 쓰기만 하는 노출 기록이라 엔티티 없이 JDBC로 저장한다. */
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
                "INSERT INTO tb_v2_recommendation_item (run_id, item_rank, shop_id, score) VALUES (?, ?, ?, ?)",
                run.items(), run.items().size(), (statement, item) -> {
                    statement.setLong(1, runId);
                    statement.setInt(2, item.rank());
                    statement.setLong(3, item.shopId());
                    statement.setDouble(4, item.score());
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

}
