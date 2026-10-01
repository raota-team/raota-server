package com.raota.mobile.account.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class AesGcmProviderTokenCipherTest {

    private final AesGcmProviderTokenCipher cipher = new AesGcmProviderTokenCipher(new MobileOAuthProperties(
            new MobileOAuthProperties.Google(List.of("google"), "https://unused.example"),
            new MobileOAuthProperties.Kakao("123456", "https://unused.example"),
            new MobileOAuthProperties.Apple("net.raota.mobile.test", "TESTTEAM01", "TESTKEY001", "unused",
                    "cm2PatRyO0rn/bD+j2wAupkmWGwmDGXdnq3PxZx59Fk=", "https://unused.example/keys",
                    "https://appleid.apple.com", "https://unused.example/token", "https://unused.example/revoke")));

    @Test
    void 같은_토큰도_서로_다른_IV로_암호화해_원본을_복원한다() {
        String first = cipher.encrypt("refresh-token-한글");
        String second = cipher.encrypt("refresh-token-한글");
        assertThat(first).startsWith("v1:").isNotEqualTo(second).doesNotContain("refresh-token");
        assertThat(cipher.decrypt(first)).isEqualTo("refresh-token-한글");
        assertThat(cipher.decrypt(second)).isEqualTo("refresh-token-한글");
    }

    @Test
    void 위변조나_알_수_없는_버전은_복호화하지_않는다() {
        String encrypted = cipher.encrypt("refresh-token");
        byte[] tampered = Base64.getDecoder().decode(encrypted.substring(3));
        tampered[tampered.length - 1] ^= 1;
        assertThatThrownBy(() -> cipher.decrypt("v1:" + Base64.getEncoder().encodeToString(tampered)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt("v2:" + encrypted.substring(3)))
            .isInstanceOf(IllegalStateException.class);
    }

}
