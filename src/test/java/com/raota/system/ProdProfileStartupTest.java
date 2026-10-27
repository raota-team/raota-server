package com.raota.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.global.file.FileUploader;
import com.raota.global.file.RoutingFileUploader;
import com.raota.support.BaseIntegrationTest;
import com.raota.web.account.infrastructure.persistence.auth.RedisRefreshTokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * 운영({@code prod}) 프로필 설정 그대로 서버가 뜨는지 본다(#83). 운영에서만 켜지는 조건부 빈,
 * {@code application-prod.yml}의 오타·누락, 설정 검증 실패를 배포 전에 잡는다.
 *
 * <p>
 * 바꾸는 값은 외부 연결 정보뿐이다. 운영에서 비밀값 파일(configtree)로 받던 값은 형식만 맞춘 가짜 값이다. MySQL·Redis는
 * {@link BaseIntegrationTest}의 Testcontainers, 벡터 스토어는 같은 곳의 목이다. 관리 포트(운영 9090)만 겹치지 않게
 * 임의 포트로 띄운다.
 * </p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            // OAuth2·외부 API 자격 증명
            "GOOGLE_CLIENT_ID=ci-google-client-id",
            "GOOGLE_CLIENT_SECRET=ci-google-client-secret",
            "KAKAO_CLIENT_ID=ci-kakao-client-id",
            "KAKAO_CLIENT_SECRET=ci-kakao-client-secret",
            "GROQ_API_KEY=ci-groq-key",
            "OPENAI_API_KEY=ci-openai-key",
            "KMA_AUTH_KEY=ci-kma-key",
            "CLOUDINARY_CLOUD_NAME=ci-cloud",
            "CLOUDINARY_API_KEY=ci-cloudinary-key",
            "CLOUDINARY_API_SECRET=ci-cloudinary-secret",
            // 객체 저장소(OCI S3 호환) — 클라이언트만 만들고 연결하지 않는다.
            "OCI_STORAGE_NAMESPACE=ci-namespace",
            "OCI_STORAGE_BUCKET=ci-bucket",
            "OCI_STORAGE_REGION=ap-chuncheon-1",
            "OCI_STORAGE_ACCESS_KEY=ci-access-key",
            "OCI_STORAGE_SECRET_KEY=ci-secret-key",
            // 벡터 DB(Oracle) — 커넥션 풀만 만들고 연결하지 않는다.
            "AI_ORACLE_DATASOURCE_URL=jdbc:oracle:thin:@localhost:1521/ci",
            "AI_ORACLE_DATASOURCE_USERNAME=ci",
            "AI_ORACLE_DATASOURCE_PASSWORD=ci",
            // 토큰 서명·암호화 키(형식만 맞춘 가짜 값, application-test.yml과 같다)
            "APP_AUTH_ACCESS_TOKEN_SECRET=ci-raota-access-token-secret-key-1234567890",
            "APP_AUTH_OAUTH2_REDIRECT_URI=http://localhost:3000/auth/callback",
            "APP_MOBILE_AUTH_ACCESS_TOKEN_SECRET=ci-mobile-v2-signing-secret-0123456789abcdef",
            "APP_MOBILE_OAUTH_APPLE_BUNDLE_ID=net.raota.mobile.ci",
            "APP_MOBILE_OAUTH_APPLE_TEAM_ID=CITEAM0001",
            "APP_MOBILE_OAUTH_APPLE_KEY_ID=CIKEY00001",
            "APP_MOBILE_OAUTH_APPLE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgw4zEakl00q5X9eIK\\n+j+GQW0W4Fb8R4Q6kEV7odtm4i6hRANCAAQJGHc0sWuVetbHV8LtpGVTsHqZ/lhz\\nSk2/4rd1RrdsfhXiOtoU7Di2FH3afJE/PQlJHgiKbs5QjhyYjsCrsDJN\\n-----END PRIVATE KEY-----",
            "APP_MOBILE_OAUTH_APPLE_TOKEN_ENCRYPTION_KEY=cm2PatRyO0rn/bD+j2wAupkmWGwmDGXdnq3PxZx59Fk="
        })
@ActiveProfiles(value = "prod", inheritProfiles = false)
class ProdProfileStartupTest extends BaseIntegrationTest {

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private ApplicationContext context;

    @Test
    void 운영_프로필로_서버가_뜨고_운영_전용_빈이_켜지며_헬스체크가_UP이다() {
        assertThat(context.getBean(FileUploader.class)).isInstanceOf(RoutingFileUploader.class);
        assertThat(context.getBeansOfType(RedisRefreshTokenStore.class)).hasSize(1);

        String health = RestClient.create()
                .get()
                .uri("http://localhost:" + managementPort + "/actuator/health")
                .retrieve()
                .body(String.class);
        assertThat(health).contains("\"status\":\"UP\"");
    }
}
