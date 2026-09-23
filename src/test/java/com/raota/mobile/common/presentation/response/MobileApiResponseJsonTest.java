package com.raota.mobile.common.presentation.response;

import com.raota.mobile.common.error.MobileErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 앱과 맺은 v2 응답 형식 계약을 Spring Boot 자동 구성 JsonMapper 기준으로 확인한다.
 * 앱이 키 존재를 전제로 파싱하므로 null 필드도 생략하지 않아야 한다.
 */
class MobileApiResponseJsonTest {

    static JsonMapper jsonMapper;

    @BeforeAll
    static void setUp() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> jsonMapper = context.getBean(JsonMapper.class));
    }

    @Test
    void 성공_응답은_error를_null로_포함한다() {
        JsonNode json = toJson(MobileApiResponse.success("ok").withRequestId("request-1"));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder("success", "data", "error", "meta");
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").asString()).isEqualTo("ok");
        assertThat(json.get("error").isNull()).isTrue();
        assertThat(json.get("meta").get("requestId").asString()).isEqualTo("request-1");
    }

    @Test
    void 실패_응답은_data를_null로_포함하고_fields는_빈_배열이다() {
        MobileError error = MobileError.of(MobileErrorCode.RESOURCE_NOT_FOUND, "없는 매장입니다.");
        JsonNode json = toJson(MobileApiResponse.failure(error).withRequestId("request-2"));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder("success", "data", "error", "meta");
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("data").isNull()).isTrue();
        assertThat(json.get("error").get("code").asString()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(json.get("error").get("message").asString()).isEqualTo("없는 매장입니다.");
        assertThat(json.get("error").get("fields").isArray()).isTrue();
        assertThat(json.get("error").get("fields").isEmpty()).isTrue();
    }

    @Test
    void 필드_오류는_field_code_message로_직렬화된다() {
        MobileError error = MobileError.of(
                MobileErrorCode.VALIDATION_ERROR,
                "입력값을 확인해 주세요.",
                List.of(new MobileFieldError("nickname", "NotBlank", "닉네임을 입력해 주세요."))
        );
        JsonNode field = toJson(MobileApiResponse.failure(error)).get("error").get("fields").get(0);

        assertThat(fieldNames(field)).containsExactly("field", "code", "message");
        assertThat(field.get("field").asString()).isEqualTo("nickname");
    }

    private JsonNode toJson(MobileApiResponse<?> response) {
        return jsonMapper.readTree(jsonMapper.writeValueAsString(response));
    }

    private static List<String> fieldNames(JsonNode node) {
        return node.propertyNames().stream().toList();
    }
}
