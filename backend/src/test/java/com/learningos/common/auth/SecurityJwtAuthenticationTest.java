package com.learningos.common.auth;

import com.learningos.config.AppProperties;
import com.learningos.config.AuthProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityJwtAuthenticationTest {

    private static final String SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String ISSUER = "learning-os";

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        UserContextHolder.clear();
    }

    @Test
    void currentUserServiceBuildsUserContextFromSpringSecurityJwtAuthentication() {
        Jwt jwt = Jwt.withTokenValue("sanitized-test-token")
                .header("alg", "HS256")
                .subject("ops_admin")
                .claim("name", "Ops Admin")
                .claim("roles", List.of("admin", "ignored_role"))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        Authentication authentication = new JwtAuthenticationToken(jwt, List.of(), "ops_admin");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CurrentUserService service = new CurrentUserService(new AppProperties("production", "X-Trace-Id"));

        UserContext currentUser = service.currentUser();

        assertThat(currentUser.userId()).isEqualTo("ops_admin");
        assertThat(currentUser.displayName()).isEqualTo("Ops Admin");
        assertThat(currentUser.roles()).containsExactly("ADMIN");
        assertThat(service.isAdmin()).isTrue();
        assertThat(service.isTeacherUser()).isFalse();
    }

    @Test
    void currentUserServiceDoesNotInferRolesFromSpringSecurityJwtSubject() {
        Jwt jwt = Jwt.withTokenValue("sanitized-test-token")
                .header("alg", "HS256")
                .subject("teacher_1")
                .claim("roles", List.of("USER"))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "teacher_1"));

        CurrentUserService service = new CurrentUserService(new AppProperties("production", "X-Trace-Id"));

        assertThat(service.currentUser().userId()).isEqualTo("teacher_1");
        assertThat(service.currentUser().roles()).containsExactly("USER");
        assertThat(service.isAdmin()).isFalse();
        assertThat(service.isTeacherUser()).isFalse();
    }

    @Test
    void devTestCurrentUserServiceDoesNotInferRolesFromSpringSecurityJwtSubject() {
        Jwt adminSubjectJwt = Jwt.withTokenValue("sanitized-test-token")
                .header("alg", "HS256")
                .subject("admin")
                .claim("roles", List.of("USER"))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        CurrentUserService service = new CurrentUserService(new AppProperties("test", "X-Trace-Id"));

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(adminSubjectJwt, List.of(), "admin")
        );
        assertThat(service.currentUser().userId()).isEqualTo("admin");
        assertThat(service.currentUser().roles()).containsExactly("USER");
        assertThat(service.isAdmin()).isFalse();

        Jwt teacherSubjectJwt = Jwt.withTokenValue("sanitized-test-token")
                .header("alg", "HS256")
                .subject("teacher_1")
                .claim("roles", List.of("USER"))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(teacherSubjectJwt, List.of(), "teacher_1")
        );
        assertThat(service.currentUser().userId()).isEqualTo("teacher_1");
        assertThat(service.currentUser().roles()).containsExactly("USER");
        assertThat(service.isTeacherUser()).isFalse();
    }

    @Test
    void devAuthFilterDoesNotHandVerifyBearerTokensAnymore() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + jwt("student_1", "Student One", List.of("STUDENT"), SECRET));
        request.addHeader("X-User-Id", "admin");
        CapturingFilterChain chain = new CapturingFilterChain();

        DevAuthFilter filter = new DevAuthFilter(
                new AppProperties("production", "X-Trace-Id"),
                new AuthProperties(SECRET, ISSUER, "", "", true)
        );
        filter.doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userDuringRequest).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void securityConfigUsesJwkSetUriWithoutRequiringLocalHmacSecret() {
        SecurityConfig securityConfig = new SecurityConfig();

        assertThatCode(() -> {
            JwtDecoder decoder = securityConfig.jwtDecoder(
                    new AuthProperties("", ISSUER, "https://idp.invalid/.well-known/jwks.json", "learning-os-api", false),
                    new AppProperties("production", "X-Trace-Id")
            );
            assertThat(decoder).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void securityConfigFailsFastInProductionWhenAuthMaterialIsMissing() {
        SecurityConfig securityConfig = new SecurityConfig();

        assertThatThrownBy(() -> securityConfig.jwtDecoder(
                new AuthProperties("", ISSUER, "", "", false),
                new AppProperties("production", "X-Trace-Id")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_JWT_SECRET or AUTH_JWK_SET_URI is required");
    }

    private static String jwt(String sub, String name, List<String> roles, String secret) throws Exception {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String roleJson = roles.stream()
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(","));
        String payload = "{\"sub\":\"" + sub + "\",\"name\":\"" + name + "\",\"roles\":[" + roleJson
                + "],\"iss\":\"" + ISSUER + "\",\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond() + "}";
        String signingInput = base64Url(header) + "." + base64Url(payload);
        return signingInput + "." + sign(signingInput, secret);
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String signingInput, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
    }

    private static class CapturingFilterChain implements jakarta.servlet.FilterChain {
        private boolean invoked;
        private UserContext userDuringRequest;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            invoked = true;
            userDuringRequest = UserContextHolder.currentUser().orElse(null);
        }
    }
}
