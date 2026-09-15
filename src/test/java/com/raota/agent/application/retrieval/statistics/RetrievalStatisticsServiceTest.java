package com.raota.agent.application.retrieval.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RetrievalStatisticsServiceTest {

    @Mock
    private JdbcTemplate oracleVectorJdbcTemplate;

    @InjectMocks
    private RetrievalStatisticsService retrievalStatisticsService;

    @Test
    void returns_zero_when_the_vector_store_is_unavailable() {
        given(oracleVectorJdbcTemplate.queryForObject(
                "SELECT count(*) FROM SPRING_AI_VECTORS",
                Long.class
        )).willThrow(new DataAccessResourceFailureException("unavailable"));

        long result = retrievalStatisticsService.countIndexedDocuments();

        assertThat(result).isZero();
    }
}
