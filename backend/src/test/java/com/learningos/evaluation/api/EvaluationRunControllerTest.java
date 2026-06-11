package com.learningos.evaluation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.evaluation.dto.EvaluationSampleRequest;
import com.learningos.evaluation.dto.EvaluationSetUpsertRequest;
import com.learningos.evaluation.application.EvaluationSetService;
import com.learningos.evaluation.repository.EvaluationRunMetricRepository;
import com.learningos.evaluation.repository.EvaluationRunRepository;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class EvaluationRunControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final EvaluationSetService evaluationSetService;
    private final EvaluationRunRepository evaluationRunRepository;
    private final EvaluationRunMetricRepository evaluationRunMetricRepository;

    EvaluationRunControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            EvaluationSetService evaluationSetService,
            EvaluationRunRepository evaluationRunRepository,
            EvaluationRunMetricRepository evaluationRunMetricRepository
    ) {
        this.mockMvc = mockMvc;
        this.evaluationSetService = evaluationSetService;
        this.evaluationRunRepository = evaluationRunRepository;
        this.evaluationRunMetricRepository = evaluationRunMetricRepository;
    }

    @Test
    void recordsRunsAndExposesPromptVersionQualityComparisonReport() throws Exception {
        String setId = createRagSet();

        postRun(setId, "agent-rag-v1", 0.55)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promptVersion").value("agent-rag-v1"))
                .andExpect(jsonPath("$.data.metrics[0].metricName").value("groundedness"));
        postRun(setId, "agent-rag-v2", 0.80)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/evaluation-runs/comparison")
                        .header("X-User-Id", "teacher")
                        .param("evaluationSetId", setId)
                        .param("promptCode", "rag-answer")
                        .param("promptVersions", "agent-rag-v1,agent-rag-v2")
                        .param("baselinePromptVersion", "agent-rag-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.baselinePromptVersion").value("agent-rag-v1"))
                .andExpect(jsonPath("$.data.rows[0].promptVersion").value("agent-rag-v1"))
                .andExpect(jsonPath("$.data.rows[1].promptVersion").value("agent-rag-v2"))
                .andExpect(jsonPath("$.data.rows[1].metrics.groundedness.average").value(0.80))
                .andExpect(jsonPath("$.data.rows[1].deltas.groundedness").value(0.25))
                .andExpect(jsonPath("$.data.winnerByMetric.groundedness").value("agent-rag-v2"))
                .andExpect(content().string(not(containsString("promptText"))))
                .andExpect(content().string(not(containsString("answerText"))))
                .andExpect(content().string(not(containsString("inputJson"))))
                .andExpect(content().string(not(containsString("rawOutput"))));
    }

    @Test
    void studentCannotComparePromptVersionQualityMetrics() throws Exception {
        String setId = createRagSet();

        mockMvc.perform(get("/api/evaluation-runs/comparison")
                        .header("X-User-Id", "student")
                        .param("evaluationSetId", setId)
                        .param("promptCode", "rag-answer")
                        .param("promptVersions", "agent-rag-v1,agent-rag-v2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void missingEvaluationSetReturnsNotFoundEnvelope() throws Exception {
        mockMvc.perform(get("/api/evaluation-runs/comparison")
                        .header("X-User-Id", "admin")
                        .param("evaluationSetId", "evs_missing")
                        .param("promptCode", "rag-answer")
                        .param("promptVersions", "agent-rag-v1,agent-rag-v2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Evaluation set not found"));
    }

    @Test
    void bearerAdminCanRecordAndCompareRunsDespiteSpoofedUserIdHeader() throws Exception {
        String setId = createRagSet("teacher", null);

        postRunAs(setId, "agent-rag-v1", 0.55, "ops_admin", "Ops Admin", List.of("ADMIN"), "student")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.promptVersion").value("agent-rag-v1"));
        postRunAs(setId, "agent-rag-v2", 0.80, "ops_admin", "Ops Admin", List.of("ADMIN"), "student")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/evaluation-runs/comparison")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "student")
                        .param("evaluationSetId", setId)
                        .param("promptCode", "rag-answer")
                        .param("promptVersions", "agent-rag-v1,agent-rag-v2")
                        .param("baselinePromptVersion", "agent-rag-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.winnerByMetric.groundedness").value("agent-rag-v2"));
    }

    @Test
    void bearerStudentSubjectAdminCannotRecordOrCompareEvaluationRuns() throws Exception {
        String setId = createRagSet("admin", null);

        postRunAs(setId, "agent-rag-v3", 0.95, "admin", "Fake Admin", List.of("STUDENT"), "admin")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/evaluation-runs/comparison")
                        .header("Authorization", "Bearer " + jwt("admin", "Fake Admin", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .param("evaluationSetId", setId)
                        .param("promptCode", "rag-answer")
                        .param("promptVersions", "agent-rag-v1,agent-rag-v2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotRecordOrCompareEvaluationRuns() throws Exception {
        String setId = createRagSet("teacher_1", null);

        postRunAs(setId, "agent-rag-v3", 0.95, "teacher_1", "Fake Teacher", List.of("USER"), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/evaluation-runs/comparison")
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Fake Teacher", List.of("USER")))
                        .param("evaluationSetId", setId)
                        .param("promptCode", "rag-answer")
                        .param("promptVersions", "agent-rag-v1,agent-rag-v2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void teacherForeignAndMissingEvaluationRunComparisonReturnSameForbiddenEnvelope() throws Exception {
        String foreignSetId = createRagSet("admin", null);

        mockMvc.perform(get("/api/evaluation-runs/comparison")
                        .header("Authorization", "Bearer " + jwt("teacher_a", "Teacher A", List.of("TEACHER")))
                        .param("evaluationSetId", foreignSetId)
                        .param("promptCode", "rag-answer")
                        .param("promptVersions", "agent-rag-v1,agent-rag-v2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/evaluation-runs/comparison")
                        .header("Authorization", "Bearer " + jwt("teacher_a", "Teacher A", List.of("TEACHER")))
                        .param("evaluationSetId", "evs_missing")
                        .param("promptCode", "rag-answer")
                        .param("promptVersions", "agent-rag-v1,agent-rag-v2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerTeacherCannotRecordRunForForeignOrMissingEvaluationSetAndDoesNotPersistRun() throws Exception {
        String foreignSetId = createRagSet("admin", null);
        long beforeRunCount = evaluationRunRepository.count();
        long beforeMetricCount = evaluationRunMetricRepository.count();

        String foreignBody = postRunAs(
                        foreignSetId,
                        "agent-rag-v-forged",
                        0.95,
                        "teacher_a",
                        "Teacher A",
                        List.of("TEACHER"),
                        "admin"
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingBody = postRunAs(
                        "evs_missing_run_forged",
                        "agent-rag-v-forged",
                        0.95,
                        "teacher_a",
                        "Teacher A",
                        List.of("TEACHER"),
                        "admin"
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignBody)
                .doesNotContain(foreignSetId)
                .doesNotContain("rag-controller-quality-admin-global")
                .doesNotContain("agent-rag-v-forged")
                .doesNotContain("trace_eval");
        assertThat(missingBody)
                .doesNotContain("evs_missing_run_forged")
                .doesNotContain("agent-rag-v-forged")
                .doesNotContain("trace_eval");
        assertThat(evaluationRunRepository.count()).isEqualTo(beforeRunCount);
        assertThat(evaluationRunMetricRepository.count()).isEqualTo(beforeMetricCount);
    }

    private org.springframework.test.web.servlet.ResultActions postRun(String setId, String promptVersion, double groundedness) throws Exception {
        String body = """
                {
                  "evaluationSetId": "%s",
                  "promptCode": "rag-answer",
                  "promptVersion": "%s",
                  "model": "mock-model",
                  "status": "SUCCEEDED",
                  "sampleCount": 10,
                  "traceId": "trace_eval",
                  "metrics": [
                    {
                      "metricName": "groundedness",
                      "metricValue": %.2f,
                      "metricUnit": "score",
                      "sampleCount": 10
                    }
                  ]
                }
                """.formatted(setId, promptVersion, groundedness);
        return mockMvc.perform(post("/api/evaluation-runs")
                .header("X-User-Id", "teacher")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions postRunAs(
            String setId,
            String promptVersion,
            double groundedness,
            String sub,
            String name,
            List<String> roles,
            String spoofedUserId
    ) throws Exception {
        String body = """
                {
                  "evaluationSetId": "%s",
                  "promptCode": "rag-answer",
                  "promptVersion": "%s",
                  "model": "mock-model",
                  "status": "SUCCEEDED",
                  "sampleCount": 10,
                  "traceId": "trace_eval",
                  "metrics": [
                    {
                      "metricName": "groundedness",
                      "metricValue": %.2f,
                      "metricUnit": "score",
                      "sampleCount": 10
                    }
                  ]
                }
                """.formatted(setId, promptVersion, groundedness);
        var request = post("/api/evaluation-runs")
                .header("Authorization", "Bearer " + jwt(sub, name, roles))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (spoofedUserId != null) {
            request.header("X-User-Id", spoofedUserId);
        }
        return mockMvc.perform(request);
    }

    private String createRagSet() {
        return createRagSet("teacher", null);
    }

    private String createRagSet(String userId, String courseId) {
        boolean admin = "admin".equals(userId);
        return evaluationSetService.upsert(userId, admin, !admin, new EvaluationSetUpsertRequest(
                "rag-controller-quality-" + userId + "-" + (courseId == null ? "global" : courseId),
                "v1",
                "RAG controller quality benchmark",
                "Controller comparison benchmark",
                "RAG_QUESTION",
                "ACTIVE",
                courseId,
                null,
                "rag-answer",
                "agent-rag-v1",
                List.of(new EvaluationSampleRequest(
                        "rag-controller-quality-001",
                        "How should RAG cite course material?",
                        List.of("doc_citation"),
                        3,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("must cite source")
                ))
        )).id();
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
