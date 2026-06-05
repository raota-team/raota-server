package com.raota.infrastructure.vector;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.oracle.OracleVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "app.ai.vector-store", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OracleVectorProperties.class)
public class VectorStoreConfig {

    @Bean(defaultCandidate = false)
    public JdbcTemplate oracleVectorJdbcTemplate(OracleVectorProperties properties) {
        var dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.url());
        dataSource.setUsername(properties.username());
        dataSource.setPassword(properties.password());
        dataSource.setDriverClassName(properties.driverClassName());
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public VectorStore vectorStore(
            @Qualifier("oracleVectorJdbcTemplate") JdbcTemplate oracleVectorJdbcTemplate,
            EmbeddingModel embeddingModel,
            OracleVectorProperties properties
    ) {
        // Spring AI의 EmbeddingModel과 Oracle Vector Store를 연결해 RAG 검색 저장소를 구성한다.
        return OracleVectorStore.builder(oracleVectorJdbcTemplate, embeddingModel)
                .indexType(parseIndexType(properties.indexType()))
                .distanceType(parseDistanceType(properties.distanceType()))
                .dimensions(properties.dimensions())
                .searchAccuracy(properties.searchAccuracy())
                .initializeSchema(properties.initializeSchema())
                .forcedNormalization(properties.forcedNormalization())
                .removeExistingVectorStoreTable(properties.removeExistingVectorStoreTable())
                .build();
    }

    private OracleVectorStore.OracleVectorStoreIndexType parseIndexType(String value) {
        if (value == null || value.isBlank()) {
            return OracleVectorStore.OracleVectorStoreIndexType.NONE;
        }
        return OracleVectorStore.OracleVectorStoreIndexType.valueOf(value.toUpperCase());
    }

    private OracleVectorStore.OracleVectorStoreDistanceType parseDistanceType(String value) {
        if (value == null || value.isBlank()) {
            return OracleVectorStore.OracleVectorStoreDistanceType.COSINE;
        }
        return OracleVectorStore.OracleVectorStoreDistanceType.valueOf(value.toUpperCase());
    }
}
