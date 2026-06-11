package com.learningos.evaluation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.evaluation.application.EvaluationSetService;
import com.learningos.evaluation.dto.EvaluationSampleRequest;
import com.learningos.evaluation.dto.EvaluationSetUpsertRequest;
import com.learningos.evaluation.repository.EvaluationSetRepository;
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
class EvaluationSetControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final EvaluationSetRepository evaluationSetRepository;
    private final EvaluationSetService evaluationSetService;

    EvaluationSetControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            EvaluationSetRepository evaluationSetRepository,
            EvaluationSetService evaluationSetService
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.evaluationSetRepository = evaluationSetRepository;
        this.evaluationSetService = evaluationSetService;
    }

    @Test
    void createsGetsAndListsEvaluationSets() throws Exception {
        String createResponse = mockMvc.perform(post("/api/evaluation-sets")
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "rag-controller",
                                  "version": "v1",
                                  "name": "RAG controller benchmark",
                                  "description": "Controller level creation",
                                  "type": "RAG_QUESTION",
                                  "status": "active",
                                  "promptCode": "rag-answer",
                                  "promptVersion": "agent-rag-v1",
                                  "samples": [
                                    {
                                      "sampleKey": "rag-api-001",
                                      "question": "How should RAG cite course material?",
                                      "expectedSourceIds": ["doc_citation"],
                                      "topK": 3,
                                      "qualityCriteria": ["must cite course source"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.type").value("RAG_QUESTION"))
                .andExpect(jsonPath("$.data.sampleCount").value(1))
                .andExpect(jsonPath("$.data.samples[0].question").value("How should RAG cite course material?"))
                .andExpect(jsonPath("$.data.samples[0].expectedSourceIds[0]").value("doc_citation"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String setId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        mockMvc.perform(get("/api/evaluation-sets/{setId}", setId)
                        .header("X-User-Id", "teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(setId))
                .andExpect(jsonPath("$.data.samples.length()").value(1))
                .andExpect(jsonPath("$.data.samples[0].topK").value(3));

        mockMvc.perform(get("/api/evaluation-sets")
                        .header("X-User-Id", "teacher")
                        .param("type", "RAG_QUESTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(setId))
                .andExpect(jsonPath("$.data[0].samples.length()").value(0));
    }

    @Test
    void missingEvaluationSetReturnsNotFoundEnvelope() throws Exception {
        mockMvc.perform(get("/api/evaluation-sets/{setId}", "evs_missing")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Evaluation set not found"));
    }

    @Test
    void studentCannotCreateEvaluationSet() throws Exception {
        mockMvc.perform(post("/api/evaluation-sets")
                        .header("X-User-Id", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "student-rag",
                                  "version": "v1",
                                  "name": "Student RAG benchmark",
                                  "type": "RAG_QUESTION",
                                  "samples": [
                                    {
                                      "sampleKey": "rag-student-001",
                                      "question": "Should not be accepted",
                                      "expectedSourceIds": ["doc_hidden"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(evaluationSetRepository.count()).isZero();
    }

    @Test
    void bearerAdminCanCreateAndListEvaluationSetsDespiteSpoofedUserIdHeader() throws Exception {
        mockMvc.perform(post("/api/evaluation-sets")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ragSetBody("bearer-admin-rag")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.code").value("bearer-admin-rag"))
                .andExpect(jsonPath("$.data.createdBy").value("ops_admin"));

        mockMvc.perform(get("/api/evaluation-sets")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "student")
                        .param("type", "RAG_QUESTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("bearer-admin-rag"));
    }

    @Test
    void bearerTeacherCanCreateEvaluationSetWithoutTeacherIdPrefix() throws Exception {
        mockMvc.perform(post("/api/evaluation-sets")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor One", List.of("TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ragSetBody("bearer-teacher-rag")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.createdBy").value("instructor_1"));
    }

    @Test
    void bearerStudentSubjectAdminCannotCreateEvaluationSet() throws Exception {
        mockMvc.perform(post("/api/evaluation-sets")
                        .header("Authorization", "Bearer " + jwt("admin", "Fake Admin", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ragSetBody("student-subject-admin-rag")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(evaluationSetRepository.count()).isZero();
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotListOrCreateEvaluationSets() throws Exception {
        mockMvc.perform(post("/api/evaluation-sets")
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Fake Teacher", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ragSetBody("user-subject-teacher-rag")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/evaluation-sets")
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Fake Teacher", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void teacherForeignAndMissingEvaluationSetReturnSameForbiddenEnvelope() throws Exception {
        String foreignSetId = evaluationSetService.upsert("admin", true, false, minimalRagSet("foreign-rag-set")).id();

        mockMvc.perform(get("/api/evaluation-sets/{setId}", foreignSetId)
                        .header("Authorization", "Bearer " + jwt("teacher_a", "Teacher A", List.of("TEACHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/evaluation-sets/{setId}", "evs_missing")
                        .header("Authorization", "Bearer " + jwt("teacher_a", "Teacher A", List.of("TEACHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private String ragSetBody(String code) {
        return """
                {
                  "code": "%s",
                  "version": "v1",
                  "name": "Bearer RAG benchmark",
                  "type": "RAG_QUESTION",
                  "status": "ACTIVE",
                  "promptCode": "rag-answer",
                  "promptVersion": "agent-rag-v1",
                  "samples": [
                    {
                      "sampleKey": "%s-001",
                      "question": "How should RAG cite course material?",
                      "expectedSourceIds": ["doc_citation"],
                      "topK": 3
                    }
                  ]
                }
                """.formatted(code, code);
    }

    private EvaluationSetUpsertRequest minimalRagSet(String code) {
        return new EvaluationSetUpsertRequest(
                code,
                "v1",
                "Foreign RAG benchmark",
                "Foreign set for anti-enumeration tests",
                "RAG_QUESTION",
                "ACTIVE",
                null,
                null,
                "rag-answer",
                "agent-rag-v1",
                List.of(new EvaluationSampleRequest(
                        code + "-001",
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
        );
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
