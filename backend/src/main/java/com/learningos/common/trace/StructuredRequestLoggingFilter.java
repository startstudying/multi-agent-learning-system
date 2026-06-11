package com.learningos.common.trace;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.observability.LearningOsMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class StructuredRequestLoggingFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE_ATTRIBUTE = "learningos.errorCode";

    private static final Logger log = LoggerFactory.getLogger(StructuredRequestLoggingFilter.class);
    private static final int MAX_FIELD_LENGTH = 128;

    private final CurrentUserService currentUserService;
    private final LearningOsMetrics metrics;

    public StructuredRequestLoggingFilter() {
        this(new CurrentUserService(), LearningOsMetrics.noop());
    }

    public StructuredRequestLoggingFilter(CurrentUserService currentUserService) {
        this(currentUserService, LearningOsMetrics.noop());
    }

    public StructuredRequestLoggingFilter(CurrentUserService currentUserService, LearningOsMetrics metrics) {
        this.currentUserService = currentUserService;
        this.metrics = metrics;
    }

    @Autowired
    public StructuredRequestLoggingFilter(
            ObjectProvider<CurrentUserService> currentUserServiceProvider,
            ObjectProvider<LearningOsMetrics> metricsProvider
    ) {
        this.currentUserService = currentUserServiceProvider.getIfAvailable(CurrentUserService::new);
        this.metrics = metricsProvider.getIfAvailable(LearningOsMetrics::noop);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
            String route = safeRoute(request);
            String errorCode = safeField(resolveErrorCode(request, response));
            metrics.recordHttpRequest(request.getMethod().toUpperCase(Locale.ROOT), route, response.getStatus(), errorCode, latencyMs);
            log.info(
                    "http_request_completed traceId={} userId={} route={} status={} latencyMs={} errorCode={}",
                    safeField(TraceContext.currentTraceId().orElse("unknown")),
                    safeField(currentUserService.currentUserId()),
                    route,
                    response.getStatus(),
                    latencyMs,
                    errorCode
            );
        }
    }

    private String safeRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String route = pattern instanceof String value && !value.isBlank()
                ? value
                : request.getMethod().toUpperCase(Locale.ROOT) + " " + request.getRequestURI();
        return removeControlCharacters(route);
    }

    private String resolveErrorCode(HttpServletRequest request, HttpServletResponse response) {
        Object errorCode = request.getAttribute(ERROR_CODE_ATTRIBUTE);
        if (errorCode instanceof String value && !value.isBlank()) {
            return value;
        }
        if (response.getStatus() < 400) {
            return ErrorCode.OK.name();
        }
        return fallbackErrorCode(response.getStatus()).name();
    }

    private ErrorCode fallbackErrorCode(int status) {
        return switch (status) {
            case 400 -> ErrorCode.VALIDATION_ERROR;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 409 -> ErrorCode.CONFLICT;
            case 503 -> ErrorCode.DEPENDENCY_UNAVAILABLE;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    private String safeField(String value) {
        String sanitized = removeControlCharacters(value);
        if (sanitized.length() <= MAX_FIELD_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_FIELD_LENGTH);
    }

    private String removeControlCharacters(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isISOControl(character)) {
                builder.append(character);
            }
        }
        if (builder.isEmpty()) {
            return "unknown";
        }
        return builder.toString();
    }
}
