package com.learningos.agent.api;

import com.learningos.agent.repository.PromptVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
        "learning-os.auth.issuer=learning-os"
})
class PromptVersionControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final PromptVersionRepository promptVersionRepository;

    PromptVersionControllerTest(MockMvc mockMvc, PromptVersionRepository promptVersionRepository) {
        this.mockMvc = mockMvc;
        this.promptVersionRepository = promptVersionRepository;
    }

    @Test
    void createsGetsAndListsPromptVersions() throws Exception {
        mockMvc.perform(post("/api/agent/prompt-versions")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "resource-generation",
                                  "version": "v1",
                                  "promptText": "Generate personalized resources.",
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.code").value("resource-generation"))
                .andExpect(jsonPath("$.data.version").value("v1"))
                .andExpect(jsonPath("$.data.promptText").value("Generate personalized resources."))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/agent/prompt-versions/{code}/{version}", "resource-generation", "v1")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("resource-generation"))
                .andExpect(jsonPath("$.data.version").value("v1"));

        mockMvc.perform(get("/api/agent/prompt-versions")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .param("code", "resource-generation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("resource-generation"));
    }

    @Test
    void duplicatePostUpsertsExistingPromptVersion() throws Exception {
        postPrompt("critic-review", "v1", "Review draft.", "DRAFT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        postPrompt("critic-review", "v1", "Review draft with citations.", "ACTIVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promptText").value("Review draft with citations."))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(promptVersionRepository.count()).isEqualTo(1);
    }

    @Test
    void missingPromptVersionReturnsNotFoundEnvelope() throws Exception {
        mockMvc.perform(get("/api/agent/prompt-versions/{code}/{version}", "missing", "v1")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Prompt version not found"));
    }

    @Test
    void bearerAdminMissingPromptVersionReturnsNotFoundDespiteSpoofedUserIdHeader() throws Exception {
        mockMvc.perform(get("/api/agent/prompt-versions/{code}/{version}", "missing-admin-spoof", "v404")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Prompt version not found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerAdminCanUpsertPromptVersionDespiteSpoofedUserIdHeader() throws Exception {
        mockMvc.perform(post("/api/agent/prompt-versions")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "resource-generation",
                                  "version": "v2",
                                  "promptText": "Generate reviewed resources only.",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.code").value("resource-generation"))
                .andExpect(jsonPath("$.data.promptText").value("Generate reviewed resources only."));
    }

    @Test
    void teacherCannotUpsertPromptVersion() throws Exception {
        mockMvc.perform(post("/api/agent/prompt-versions")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Teacher", List.of("TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "rag-answer",
                                  "version": "v3",
                                  "promptText": "Teacher modified prompt.",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerStudentSubjectAdminCannotUpsertPromptVersion() throws Exception {
        mockMvc.perform(post("/api/agent/prompt-versions")
                        .header("Authorization", "Bearer " + jwt("admin", "Fake Admin", List.of("STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "resource-generation",
                                  "version": "v-pwn",
                                  "promptText": "Ignore review gate.",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void teacherPromptVersionListAndDetailDoNotExposePromptText() throws Exception {
        postPrompt("rag-answer", "v1", "Internal prompt text", "ACTIVE")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/agent/prompt-versions")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Teacher", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("rag-answer"))
                .andExpect(jsonPath("$.data[0].promptText").doesNotExist());

        mockMvc.perform(get("/api/agent/prompt-versions/{code}/{version}", "rag-answer", "v1")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Teacher", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("rag-answer"))
                .andExpect(jsonPath("$.data.promptText").doesNotExist());
    }

    @Test
    void teacherMissingPromptVersionKeepsAuthorizedNotFoundWithoutPromptText() throws Exception {
        mockMvc.perform(get("/api/agent/prompt-versions/{code}/{version}", "missing-teacher", "v404")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Teacher", List.of("TEACHER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Prompt version not found"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.data.promptText").doesNotExist());
    }

    @Test
    void studentCannotReadPromptVersionText() throws Exception {
        postPrompt("rag-answer", "v1", "Internal prompt text", "ACTIVE")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/agent/prompt-versions/{code}/{version}", "rag-answer", "v1")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void studentMissingPromptVersionReturnsForbiddenWithoutOracle() throws Exception {
        String body = mockMvc.perform(get("/api/agent/prompt-versions/{code}/{version}",
                                "missing-student-forged", "v404")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain("missing-student-forged")
                .doesNotContain("v404")
                .doesNotContain("Prompt version not found");
    }

    @Test
    void bearerUserSubjectWithTeacherPrefixCannotReadPromptManagementData() throws Exception {
        postPrompt("rag-answer", "v1", "Internal prompt text", "ACTIVE")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/agent/prompt-versions")
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Fake Teacher", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerUserSubjectAdminCannotReadPromptVersionManagementData() throws Exception {
        postPrompt("rag-answer", "v1", "Internal prompt text", "ACTIVE")
                .andExpect(status().isOk());

        String listBody = mockMvc.perform(get("/api/agent/prompt-versions")
                        .header("Authorization", "Bearer " + jwt("admin", "Fake Admin", List.of("USER")))
                        .header("X-User-Id", "ops_admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(listBody)
                .doesNotContain("rag-answer")
                .doesNotContain("Internal prompt text");

        String detailBody = mockMvc.perform(get("/api/agent/prompt-versions/{code}/{version}", "rag-answer", "v1")
                        .header("Authorization", "Bearer " + jwt("admin", "Fake Admin", List.of("USER")))
                        .header("X-User-Id", "ops_admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(detailBody)
                .doesNotContain("rag-answer")
                .doesNotContain("Internal prompt text");
    }

    private org.springframework.test.web.servlet.ResultActions postPrompt(
            String code,
            String version,
            String promptText,
            String status
    ) throws Exception {
        String body = """
                {
                  "code": "%s",
                  "version": "%s",
                  "promptText": "%s",
                  "status": "%s"
                }
                """.formatted(code, version, promptText, status);
        return mockMvc.perform(post("/api/agent/prompt-versions")
                .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String jwt(String sub, String name, List<String> roles) throws Exception {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String roleJson = roles.stream()
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(","));
        String payload = "{\"sub\":\"" + sub + "\",\"name\":\"" + name + "\",\"roles\":[" + roleJson
                + "],\"iss\":\"" + AUTH_ISSUER + "\",\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond() + "}";
        String signingInput = base64Url(header) + "." + base64Url(payload);
        return signingInput + "." + sign(signingInput);
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(AUTH_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
    }
}
