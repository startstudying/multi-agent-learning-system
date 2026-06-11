package com.learningos.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.trace.StructuredRequestLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        request.setAttribute(StructuredRequestLoggingFilter.ERROR_CODE_ATTRIBUTE, ErrorCode.FORBIDDEN.name());
        response.setStatus(ErrorCode.FORBIDDEN.httpStatus().value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.of(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage(), null)
        ));
    }
}
