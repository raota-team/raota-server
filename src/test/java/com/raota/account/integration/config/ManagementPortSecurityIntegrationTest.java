package com.raota.account.integration.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.support.BaseIntegrationTest;
import java.net.HttpURLConnection;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class ManagementPortSecurityIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int applicationPort;

    @LocalManagementPort
    private int managementPort;

    @DynamicPropertySource
    static void managementProperties(DynamicPropertyRegistry registry) {
        registry.add("management.server.port", () -> 0);
        registry.add("management.endpoints.web.exposure.include", () -> "health,prometheus");
    }

    @Test
    void Prometheus는_별도_관리_포트에서만_인증_없이_노출한다() throws Exception {
        assertThat(status(managementPort, "/actuator/prometheus")).isEqualTo(200);
        assertThat(status(applicationPort, "/actuator/prometheus")).isNotEqualTo(200);
    }

    private int status(int port, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + path)
                .toURL()
                .openConnection();
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(5_000);
        connection.setInstanceFollowRedirects(false);
        return connection.getResponseCode();
    }
}
