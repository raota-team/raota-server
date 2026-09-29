package com.raota.mobile.account.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoAccessTokenVerifierTest {

    private MockRestServiceServer server;

    private KakaoAccessTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new KakaoAccessTokenVerifier(builder,
                new MobileOAuthProperties(new MobileOAuthProperties.Google(List.of("client"), "https://unused.example"),
                        new MobileOAuthProperties.Kakao("123456", "https://kakao.example")));
    }

    @Test
    void 앱이_맞는_토큰의_회원_ID와_이메일을_반환한다() {
        server.expect(once(), requestTo("https://kakao.example/v1/user/access_token_info"))
            .andExpect(header("Authorization", "Bearer kakao-token"))
            .andRespond(withSuccess("{\"app_id\":123456}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://kakao.example/v2/user/me"))
            .andExpect(header("Authorization", "Bearer kakao-token"))
            .andRespond(withSuccess("{\"id\":987654,\"kakao_account\":{\"email\":\"member@example.com\"}}",
                    MediaType.APPLICATION_JSON));

        var identity = verifier.verify(credential("kakao-token"));

        assertThat(identity.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(identity.subject()).isEqualTo("987654");
        assertThat(identity.email()).isEqualTo("member@example.com");
        server.verify();
    }

    @Test
    void 유효하지_않은_토큰의_401은_인증_오류다() {
        server.expect(once(), requestTo("https://kakao.example/v1/user/access_token_info"))
            .andRespond(withUnauthorizedRequest());

        assertInvalid("kakao-token");
        server.verify();
    }

    @Test
    void 다른_카카오_앱에서_발급한_토큰은_회원_정보를_읽지_않는다() {
        server.expect(once(), requestTo("https://kakao.example/v1/user/access_token_info"))
            .andRespond(withSuccess("{\"app_id\":999999}", MediaType.APPLICATION_JSON));

        assertInvalid("kakao-token");
        server.verify();
    }

    @Test
    void 액세스_토큰을_누락하면_입력_오류다() {
        assertThatThrownBy(() -> verifier.verify(credential(null))).isInstanceOfSatisfying(MobileException.class,
                exception -> assertThat(exception.code()).isEqualTo(MobileErrorCode.VALIDATION_ERROR));
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> verifier.verify(credential(token))).isInstanceOfSatisfying(MobileException.class,
                exception -> assertThat(exception.code()).isEqualTo(MobileErrorCode.OAUTH_CREDENTIAL_INVALID));
    }

    private static SocialCredential credential(String accessToken) {
        return new SocialCredential(null, accessToken, null, null);
    }

}
