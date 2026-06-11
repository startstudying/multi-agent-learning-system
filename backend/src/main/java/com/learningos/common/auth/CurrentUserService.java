package com.learningos.common.auth;

import com.learningos.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CurrentUserService {

    static final String DEV_USER_ID = "dev_user";
    private static final UserContext DEV_USER_CONTEXT = new UserContext(DEV_USER_ID, DEV_USER_ID, List.of("USER"));
    private static final UserContext UNAUTHENTICATED_CONTEXT =
            new UserContext("unauthenticated", "unauthenticated", List.of("USER"));

    private final AppProperties appProperties;

    public CurrentUserService() {
        this(new AppProperties("dev", "X-Trace-Id"));
    }

    @Autowired
    public CurrentUserService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public UserContext currentUser() {
        UserContext securityContextUser = currentUserFromSecurityContext();
        if (securityContextUser != null) {
            return securityContextUser;
        }
        return UserContextHolder.currentUser().orElseGet(this::fallbackUserContext);
    }

    public String currentUserId() {
        return currentUser().userId();
    }

    public boolean isAdmin() {
        UserContext currentUser = currentUser();
        return hasRole(currentUser, "ADMIN")
                || (allowsLegacySubjectInferenceForHolderContext() && "admin".equals(currentUser.userId()));
    }

    public boolean isTeacherUser() {
        UserContext currentUser = currentUser();
        String userId = currentUser.userId();
        return hasRole(currentUser, "TEACHER")
                || (allowsLegacySubjectInferenceForHolderContext()
                && ("teacher".equals(userId) || (userId != null && userId.startsWith("teacher_"))));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }

    private UserContext currentUserFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            return null;
        }
        Jwt jwt = jwtAuthentication.getToken();
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return null;
        }
        String displayName = jwt.getClaimAsString("name");
        return new UserContext(subject, displayName == null || displayName.isBlank() ? subject : displayName, roles(jwt.getClaim("roles")));
    }

    private List<String> roles(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of("USER");
        }
        List<String> roles = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof String role) {
                String normalized = normalizeRole(role);
                if (normalized != null && !roles.contains(normalized)) {
                    roles.add(normalized);
                }
            }
        }
        if (roles.isEmpty()) {
            return List.of("USER");
        }
        return List.copyOf(roles);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if ("ROLE_ADMIN".equals(normalized)) {
            return "ADMIN";
        }
        if ("ROLE_TEACHER".equals(normalized)) {
            return "TEACHER";
        }
        if ("ROLE_STUDENT".equals(normalized)) {
            return "STUDENT";
        }
        if ("ROLE_USER".equals(normalized)) {
            return "USER";
        }
        return switch (normalized) {
            case "ADMIN", "TEACHER", "STUDENT", "USER" -> normalized;
            default -> null;
        };
    }

    private boolean allowsLegacyRoleInference() {
        String environment = appProperties.environment();
        return "dev".equalsIgnoreCase(environment) || "test".equalsIgnoreCase(environment);
    }

    private boolean allowsLegacySubjectInferenceForHolderContext() {
        return allowsLegacyRoleInference() && currentUserFromSecurityContext() == null;
    }

    private UserContext fallbackUserContext() {
        if (allowsLegacyRoleInference()) {
            return DEV_USER_CONTEXT;
        }
        return UNAUTHENTICATED_CONTEXT;
    }
}
