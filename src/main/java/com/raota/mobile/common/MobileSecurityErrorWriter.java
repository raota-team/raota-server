package com.raota.mobile.common;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.global.presentation.v2.V2ApiResponse;
import com.raota.global.presentation.v2.V2Error;
import com.raota.global.presentation.v2.V2ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 필터 단계에서 발생한 401·403을 v2 응답 형식으로 쓴다.
 *
 * <p>이 오류는 컨트롤러 밖에서 나므로 {@code @RestControllerAdvice}가 잡지 못한다.
 * 그래서 별도 writer가 필요하다.</p>
 */
@Component
@RequiredArgsConstructor
public class MobileSecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, V2ErrorCode code, String message) throws IOException {
        response.setStatus(code.status().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                V2ApiResponse.failure(V2Error.of(code, message), RequestIdFilter.currentRequestId())
        );
    }
}
