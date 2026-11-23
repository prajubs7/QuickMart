package com.quickmart.config;

import com.quickmart.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * 401 and 403 raised INSIDE the security filter chain, written as our standard ErrorResponse.
 *
 * Why not GlobalExceptionHandler? The filter chain runs before the DispatcherServlet,
 * so @RestControllerAdvice never sees these exceptions. Without this class Spring
 * returns an empty body (or its own format), and the mobile app's error parser breaks.
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    // 401: no token, malformed token, bad signature, expired.
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        boolean badToken = ex instanceof OAuth2AuthenticationException;
        // RFC 6750: tell the client which scheme to use, and whether the token it sent was the problem.
        res.setHeader(HttpHeaders.WWW_AUTHENTICATE, badToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        write(res, req, HttpStatus.UNAUTHORIZED,
                badToken ? "Invalid or expired access token" : "Authentication required");
    }

    // 403: valid token, wrong role (e.g. a CUSTOMER calling a category write).
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        write(res, req, HttpStatus.FORBIDDEN, "You don't have permission for this action");
    }

    private void write(HttpServletResponse res, HttpServletRequest req, HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(res.getOutputStream(),
                ErrorResponse.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI()));
    }
}
