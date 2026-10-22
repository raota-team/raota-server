package com.raota.agent.application.mobile;

import com.raota.agent.application.mobile.command.MobileRecommendationCommand;
import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 추천 응답과 분리해 노출 기록을 남긴다. 저장은 자기 트랜잭션에서 하고, 실패해도 추천은 성공해야 하므로 예외를 삼키고 기록 ID 없이 돌아간다.
 */
@Slf4j
@Component
class MobileRecommendationLogger {

    static final String ALGORITHM_VERSION = "soup-filter+nearest10+vector-similarity";

    private final MobileRecommendationRunRepository runs;

    private final ObjectMapper mapper;

    private final Clock clock;

    private final String indexVersion;

    private final String modelVersion;

    MobileRecommendationLogger(MobileRecommendationRunRepository runs, ObjectMapper mapper, Clock clock,
            @Value("${app.rag.evaluation.vector-index-version:unknown}") String indexVersion,
            @Value("${spring.ai.openai.embedding.model:unknown}") String modelVersion) {
        this.runs = runs;
        this.mapper = mapper;
        this.clock = clock;
        this.indexVersion = indexVersion;
        this.modelVersion = modelVersion;
    }

    /** 기록 ID를 돌려준다. 저장하지 못하면 {@code null}이다. */
    String record(Long userId, MobileRecommendationCommand command, int candidateCount,
            List<MobileRecommendationRun.Item> shown, long latencyMs) {
        try {
            return runs
                .save(new MobileRecommendationRun(userId, conditions(command), coarse(command.latitude(), 2),
                        coarse(command.longitude(), 2), candidateCount, ALGORITHM_VERSION, indexVersion, modelVersion,
                        Math.toIntExact(Math.min(latencyMs, Integer.MAX_VALUE)),
                        Instant.now(clock).truncatedTo(ChronoUnit.MICROS), shown))
                .toString();
        }
        catch (RuntimeException exception) {
            log.warn("추천 노출 기록을 남기지 못했습니다: userId={}", userId, exception);
            return null;
        }
    }

    private String conditions(MobileRecommendationCommand command) {
        Map<String, String> conditions = new LinkedHashMap<>();
        conditions.put("soup", command.soup());
        conditions.put("mood", command.mood());
        conditions.put("priority", command.priority());
        conditions.put("text", command.text());
        conditions.put("region", command.region());
        conditions.values().removeIf(value -> value == null || value.isBlank());
        return mapper.writeValueAsString(conditions);
    }

    private static BigDecimal coarse(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

}
