package com.raota.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtCodecTest {

    private static final String SECRET = "test-codec-key-signature-secret-0123456789abcdef";

    private final JwtCodec codec = new JwtCodec("raota-mobile-test", SECRET);

    @Test
    void 발급한_토큰의_서명과_발급자를_검증하고_subject를_반환한다() {
        String token = codec.issue("42", Duration.ofMinutes(30), Map.of("scope", "test"));

        assertThat(codec.verifySubject(token)).isEqualTo("42");
    }

    @Test
    void 같은_키라도_발급자가_다르면_거부한다() {
        String token = new JwtCodec("another-issuer", SECRET).issue("42", Duration.ofMinutes(30), Map.of());

        assertThatThrownBy(() -> codec.verifySubject(token)).isInstanceOf(InvalidJwtTokenException.class);
    }

    @Test
    void 다른_키로_서명된_토큰은_거부한다() {
        String token = new JwtCodec("raota-mobile-test", "different-signing-secret-0123456789abcdef").issue("42",
                Duration.ofMinutes(30), Map.of());

        assertThatThrownBy(() -> codec.verifySubject(token)).isInstanceOf(InvalidJwtTokenException.class);
    }

    @Test
    void 만료된_토큰은_전용_예외로_알린다() {
        String token = codec.issue("42", Duration.ofSeconds(-60), Map.of());

        assertThatThrownBy(() -> codec.verifySubject(token)).isInstanceOf(ExpiredJwtTokenException.class);
    }

    @Test
    void 비어_있거나_해석되지_않은_placeholder가_남은_서명_키는_거부한다() {
        assertThatThrownBy(() -> new JwtCodec("raota-mobile-test", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtCodec("raota-mobile-test", "${APP_MOBILE_AUTH_ACCESS_TOKEN_SECRET}"))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
