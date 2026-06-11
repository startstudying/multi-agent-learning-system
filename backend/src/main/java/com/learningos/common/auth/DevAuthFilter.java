package com.learningos.common.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.learningos.config.AppProperties;
import com.learningos.config.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Dev/test auth fallback. Bearer tokens are handled by Spring Security.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DevAuthFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    private static final String DEFAULT_USER_ID = "dev_user";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {
    };

    private final AppProperties appProperties;
    private final AuthProperties authProperties;

    public DevAuthFilter(AppProperties appProperties, AuthProperties authProperties) {
        this.appProperties = appProperties;
        this.authProperties = authProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        UserContextHolder.clear();
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!allowsDevHeaderFallback()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            userId = DEFAULT_USER_ID;
        }
        runWithUserContext(new UserContext(userId, userId, deriveDevRoles(userId)), request, response, filterChain);
    }

    private void runWithUserContext(
            UserContext userContext,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        UserContextHolder.setCurrentUser(userContext);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private List<String> deriveDevRoles(String userId) {
        if ("admin".equals(userId)) {
            return List.of("ADMIN");
        }
        if ("teacher".equals(userId) || (userId != null && userId.startsWith("teacher_"))) {
            return List.of("TEACHER");
        }
        return List.of("USER");
    }

    private boolean allowsDevHeaderFallback() {
        String environment = appProperties.environment();
        return authProperties.devHeaderFallbackEnabled()
                && ("dev".equalsIgnoreCase(environment) || "test".equalsIgnoreCase(environment));
    }
}
