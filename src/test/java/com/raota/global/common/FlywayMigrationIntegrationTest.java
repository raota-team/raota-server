package com.raota.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;



class FlywayMigrationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("모든 Flyway 마이그레이션 스크립트가 에러 없이 실행되고, JPA 엔티티와 스키마가 일치한다.")
    void flywayMigrationAndHibernateValidationTest() {
        // 이 테스트는 Spring Context가 로드되는 과정에서 Flyway 마이그레이션과
        // Hibernate Schema Validation(ddl-auto: validate)을 자동으로 수행합니다.
        // 따라서 테스트가 통과한다는 것은 스키마 정합성이 보장됨을 의미합니다.
    }
}
