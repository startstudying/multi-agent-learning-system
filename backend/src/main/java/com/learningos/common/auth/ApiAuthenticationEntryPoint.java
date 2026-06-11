package com.learningos.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.trace.StructuredRequestLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        request.setAttribute(StructuredRequestLoggingFilter.ERROR_CODE_ATTRIBUTE, ErrorCode.UNAUTHORIZED.name());
        UserContextHolder.clear();
        response.setStatus(ErrorCode.UNAUTHORIZED.httpStatus().value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.of(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.defaultMessage(), null)
        ));
    }
}
