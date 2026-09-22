package com.raota.mobile.common;

import com.raota.global.presentation.v2.V2ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 권한이 없는 `/api/v2` 요청에 403을 v2 형식으로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class MobileAccessDeniedHandler implements AccessDeniedHandler {

    private final MobileSecurityErrorWriter errorWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        errorWriter.write(response, V2ErrorCode.FORBIDDEN, "권한이 없습니다.");
    }
}
