package com.raota.integration;

import com.raota.support.BaseIntegrationTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("모든 Flyway 마이그레이션 스크립트가 에러 없이 실행되고, JPA 엔티티와 스키마가 일치한다.")
    void flywayMigrationAndHibernateValidationTest() {
        // 이 테스트는 Spring Context가 로드되는 과정에서 Flyway 마이그레이션과
        // Hibernate Schema Validation(ddl-auto: validate)을 자동으로 수행합니다.
        // 따라서 테스트가 통과한다는 것은 스키마 정합성이 보장됨을 의미합니다.
    }

    @Test
    @DisplayName("기존 인증샷 데이터가 V16에서 라멘로그 데이터로 보존된다.")
    void proofPictureDataIsPreservedWhenUpgradedToRamenLog() throws Exception {
        String databaseName = "raota_migration_" + UUID.randomUUID().toString().replace("-", "");
        String adminUrl = MYSQL_CONTAINER.getJdbcUrl();
        String migrationUrl = adminUrl.replace("/raota", "/" + databaseName);

        try (Connection connection = DriverManager.getConnection(
                adminUrl,
                MYSQL_CONTAINER.getUsername(),
                MYSQL_CONTAINER.getPassword()
        ); Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + databaseName);
        }

        try {
            Flyway.configure()
                    .dataSource(migrationUrl, MYSQL_CONTAINER.getUsername(), MYSQL_CONTAINER.getPassword())
                    .locations("classpath:db/migration")
                    .target(MigrationVersion.fromVersion("15"))
                    .load()
                    .migrate();

            try (Connection connection = DriverManager.getConnection(
                    migrationUrl,
                    MYSQL_CONTAINER.getUsername(),
                    MYSQL_CONTAINER.getPassword()
            ); Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO tb_ramen_proof_picture
                            (ramen_shop_id, member_id, image_name, image_url, description, uploaded_at, menu_name, is_deleted)
                        VALUES
                            (10, 20, 'legacy', 'proof/legacy.jpg', '기존 인증샷', NOW(6), '시오라멘', false)
                        """);
            }

            Flyway.configure()
                    .dataSource(migrationUrl, MYSQL_CONTAINER.getUsername(), MYSQL_CONTAINER.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (Connection connection = DriverManager.getConnection(
                    migrationUrl,
                    MYSQL_CONTAINER.getUsername(),
                    MYSQL_CONTAINER.getPassword()
            ); Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("""
                         SELECT menu_name, ramen_type, image_url, note, revisit, is_public, is_deleted
                         FROM tb_ramen_log
                         WHERE image_name = 'legacy'
                         """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("menu_name")).isEqualTo("시오라멘");
                assertThat(result.getString("ramen_type")).isEqualTo("기타");
                assertThat(result.getString("image_url")).isEqualTo("proof/legacy.jpg");
                assertThat(result.getString("note")).isEqualTo("기존 인증샷");
                assertThat(result.getString("revisit")).isEqualTo("SOMETIMES");
                assertThat(result.getBoolean("is_public")).isTrue();
                assertThat(result.getBoolean("is_deleted")).isFalse();
            }
        } finally {
            try (Connection connection = DriverManager.getConnection(
                    adminUrl,
                    MYSQL_CONTAINER.getUsername(),
                    MYSQL_CONTAINER.getPassword()
            ); Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + databaseName);
            }
        }
    }
}
