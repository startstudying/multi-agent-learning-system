package com.learningos.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
        "learning-os.auth.issuer=learning-os",
        "learning-os.model-provider.encryption-key=test-model-provider-key"
})
class BusinessPermissionMatrixRegressionTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;

    BusinessPermissionMatrixRegressionTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void studentCannotListModelProviders() throws Exception {
        mockMvc.perform(get("/api/admin/model-providers")
                        .header("Authorization", "Bearer " + jwt("stu_001", "Student", List.of("STUDENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherCannotAccessTokenBudgetGovernance() throws Exception {
        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("Authorization", "Bearer " + jwt("teacher_001", "Teacher", List.of("TEACHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherCannotAccessPersistedOpsAlerts() throws Exception {
        mockMvc.perform(get("/api/analytics/ops/alerts/persisted")
                        .header("Authorization", "Bearer " + jwt("teacher_001", "Teacher", List.of("TEACHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanAccessModelProvidersAndPersistedAlerts() throws Exception {
        String adminJwt = jwt("ops_admin", "Ops Admin", List.of("ADMIN"));
        mockMvc.perform(get("/api/admin/model-providers").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/ops/alerts/persisted").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());
    }

    private static String jwt(String userId, String displayName, List<String> roles) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + userId + "\",\"name\":\"" + displayName + "\",\"roles\":["
                        + roles.stream().map(role -> "\"" + role + "\"").collect(Collectors.joining(","))
                        + "],\"iss\":\"" + AUTH_ISSUER + "\",\"exp\":" + (Instant.now().getEpochSecond() + 3600)
                        + "}").getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(AUTH_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return header + "." + payload + "." + signature;
    }
}
