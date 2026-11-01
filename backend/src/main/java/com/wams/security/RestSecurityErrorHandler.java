package com.wams.security;

import com.wams.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

/**
 * Writes filter-chain authentication and authorization failures using the same
 * {@link ApiError} envelope as controller/service exceptions. Calling
 * {@code sendError} would start an ERROR dispatch and could turn a genuine 403
 * into an empty 401 when the error route is itself protected.
 */
@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException exception) throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "Access denied");
    }

    private void write(HttpServletResponse response,
                       HttpServletRequest request,
                       HttpStatus status,
                       String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build());
    }
}
