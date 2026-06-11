package com.learningos.common.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.app.environment=production",
        "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
        "learning-os.auth.issuer=learning-os",
        "learning-os.auth.audience=learning-os-api"
})
class SecurityFilterChainTest {

    private static final String SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String ISSUER = "learning-os";
    private static final String AUDIENCE = "learning-os-api";

    private final MockMvc mockMvc;

    SecurityFilterChainTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void productionRequestWithoutBearerTokenRejectsSpoofedUserHeaderWithSanitizedEnvelope() throws Exception {
        mockMvc.perform(get("/api/users/anything")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("admin"))));
    }

    @Test
    void productionInvalidBearerTokenRejectsSpoofedUserHeaderWithSanitizedEnvelope() throws Exception {
        mockMvc.perform(get("/api/users/anything")
                        .header("Authorization", "Bearer " + jwt("student_1", "Student One", List.of("STUDENT"), "wrong-secret", ISSUER, Instant.now().plusSeconds(3600)))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("admin"))));
    }

    @Test
    void productionWrongIssuerBearerTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/anything")
                        .header("Authorization", "Bearer " + jwt("student_1", "Student One", List.of("STUDENT"), SECRET, "wrong-issuer", Instant.now().plusSeconds(3600)))
                        .header("X-User-Id", "student_1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void productionExpiredBearerTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/anything")
                        .header("Authorization", "Bearer " + jwt("student_1", "Student One", List.of("STUDENT"), SECRET, ISSUER, Instant.now().minusSeconds(60)))
                        .header("X-User-Id", "student_1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void productionWrongAudienceBearerTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/anything")
                        .header("Authorization", "Bearer " + jwt("student_1", "Student One", List.of("STUDENT"), SECRET, ISSUER, "other-api", Instant.now().plusSeconds(3600)))
                        .header("X-User-Id", "student_1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void productionValidBearerTokenUsesJwtRolesAndIgnoresSpoofedUserHeader() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN"), SECRET, ISSUER, Instant.now().plusSeconds(3600)))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void productionBearerSubjectNameDoesNotGrantAdminRole() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"), SECRET, ISSUER, Instant.now().plusSeconds(3600)))
                        .header("X-User-Id", "ops_admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private static String jwt(String sub, String name, List<String> roles, String secret, String issuer, Instant expiresAt) throws Exception {
        return jwt(sub, name, roles, secret, issuer, AUDIENCE, expiresAt);
    }

    private static String jwt(String sub, String name, List<String> roles, String secret, String issuer, String audience, Instant expiresAt) throws Exception {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String roleJson = roles.stream()
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(","));
        String payload = "{\"sub\":\"" + sub + "\",\"name\":\"" + name + "\",\"roles\":[" + roleJson
                + "],\"iss\":\"" + issuer + "\",\"aud\":[\"" + audience + "\"],\"exp\":" + expiresAt.getEpochSecond() + "}";
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
}
