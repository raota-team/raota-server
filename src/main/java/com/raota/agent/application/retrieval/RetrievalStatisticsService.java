package com.raota.agent.application.retrieval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RetrievalStatisticsService {

    private final JdbcTemplate oracleVectorJdbcTemplate;

    public RetrievalStatisticsService(
            @Qualifier("oracleVectorJdbcTemplate") JdbcTemplate oracleVectorJdbcTemplate
    ) {
        this.oracleVectorJdbcTemplate = oracleVectorJdbcTemplate;
    }

    public long countIndexedDocuments() {
        try {
            Long count = oracleVectorJdbcTemplate.queryForObject(
                    "SELECT count(*) FROM SPRING_AI_VECTORS",
                    Long.class
            );
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            log.warn("Vector 문서 수 조회에 실패하여 0으로 처리합니다.", exception);
            return 0;
        }
    }
}
