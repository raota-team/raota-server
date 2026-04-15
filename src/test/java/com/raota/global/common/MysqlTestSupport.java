package com.raota.global.common;

import org.flywaydb.core.Flyway;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class MysqlTestSupport {

    @Container
    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("raota")
            .withUsername("root")
            .withPassword("password");

    static {
        MYSQL_CONTAINER.start();
        // 스프링이 뜨기 전에 Flyway를 수동으로 강제 실행
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL_CONTAINER.getJdbcUrl(), MYSQL_CONTAINER.getUsername(), MYSQL_CONTAINER.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }
}
