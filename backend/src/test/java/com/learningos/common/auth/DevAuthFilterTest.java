package com.learningos.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import com.learningos.config.AppProperties;
import com.learningos.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DevAuthFilterTest {

    private static final String SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String ISSUER = "learning-os";

    @Test
    void usesUserIdHeaderAsCurrentUserAndClearsContextAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-User-Id", "alice");
        CapturingFilterChain chain = new CapturingFilterChain();

        devFilter().doFilter(request, response, chain);

        assertThat(chain.userDuringRequest).isNotNull();
        assertThat(chain.userDuringRequest.userId()).isEqualTo("alice");
        assertThat(chain.userDuringRequest.displayName()).isEqualTo("alice");
        assertThat(chain.userDuringRequest.roles()).containsExactly("USER");
        assertThat(UserContextHolder.currentUser()).isEmpty();
    }

    @Test
    void defaultsToDevUserWhenHeaderIsMissingAndClearsContextAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        devFilter().doFilter(request, response, chain);
        assertThat(chain.invoked).isTrue();

        assertThat(chain.userDuringRequest).isNotNull();
        assertThat(chain.userDuringRequest.userId()).isEqualTo("dev_user");
        assertThat(chain.userDuringRequest.displayName()).isEqualTo("dev_user");
        assertThat(chain.userDuringRequest.roles()).containsExactly("USER");
        assertThat(UserContextHolder.currentUser()).isEmpty();
    }

    @Test
    void clearsContextWhenDownstreamFails() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-User-Id", "alice");

        try {
            devFilter().doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                throw new ServletException("boom");
            });
        } catch (ServletException ignored) {
        }

        assertThat(UserContextHolder.currentUser()).isEmpty();
    }

    @Test
    void validBearerTokenOverridesSpoofedUserIdHeaderInTestEnvironment() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + jwt("student_1", "Student One", List.of("STUDENT"), SECRET));
        request.addHeader("X-User-Id", "admin");
        CapturingFilterChain chain = new CapturingFilterChain();

        testFilter().doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userDuringRequest).isNull();
        assertThat(UserContextHolder.currentUser()).isEmpty();
    }

    @Test
    void bearerTokenIsNotHandledByDevAuthFilterAndCannotFallbackToUserIdHeaderInTestEnvironment() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + jwt("student_1", "Student One", List.of("STUDENT"), "wrong-secret"));
        request.addHeader("X-User-Id", "admin");
        CapturingFilterChain chain = new CapturingFilterChain();

        testFilter().doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userDuringRequest).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).doesNotContain("admin");
        assertThat(UserContextHolder.currentUser()).isEmpty();
    }

    @Test
    void productionMissingBearerTokenDoesNotCreateHeaderIdentity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-User-Id", "admin");
        CapturingFilterChain chain = new CapturingFilterChain();

        productionFilter().doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userDuringRequest).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).doesNotContain("admin");
        assertThat(UserContextHolder.currentUser()).isEmpty();
    }

    @Test
    void stagingMissingBearerTokenDoesNotCreateHeaderIdentity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-User-Id", "admin");
        CapturingFilterChain chain = new CapturingFilterChain();

        stagingFilter().doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userDuringRequest).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).doesNotContain("admin");
        assertThat(UserContextHolder.currentUser()).isEmpty();
    }

    @Test
    void productionValidBearerTokenIgnoresSpoofedUserIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + jwt("student_1", "Student One", List.of("STUDENT"), SECRET));
        request.addHeader("X-User-Id", "admin");
        CapturingFilterChain chain = new CapturingFilterChain();

        productionFilter().doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userDuringRequest).isNull();
        assertThat(UserContextHolder.currentUser()).isEmpty();
    }

    private static DevAuthFilter devFilter() {
        return filter("dev");
    }

    private static DevAuthFilter testFilter() {
        return filter("test");
    }

    private static DevAuthFilter stagingFilter() {
        return filter("staging");
    }

    private static DevAuthFilter productionFilter() {
        return filter("production");
    }

    private static DevAuthFilter filter(String environment) {
        return new DevAuthFilter(new AppProperties(environment, "X-Trace-Id"), new AuthProperties(SECRET, ISSUER));
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

    private static class CapturingFilterChain implements FilterChain {
        private boolean invoked;
        private UserContext userDuringRequest;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            invoked = true;
            userDuringRequest = UserContextHolder.currentUser().orElse(null);
        }
    }
}
