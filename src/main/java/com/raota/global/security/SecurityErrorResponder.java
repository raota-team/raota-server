package com.raota.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

public interface SecurityErrorResponder {

    boolean supports(HttpServletRequest request);

    void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException;

    void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException;

}
