package com.learningos.aiqa.api;

import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.application.RagQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
        "learning-os.auth.issuer=learning-os"
})
class AiQaControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;

    @MockBean
    private RagQueryService ragQueryService;

    AiQaControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void aiQaEndpointReturnsUnifiedContractWithSafeThinkingSummary() throws Exception {
        when(ragQueryService.queryWithRequestId(
                "alice",
                false,
                false,
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "req_ai_qa_1"
        )).thenReturn(new RagQueryResponse(
                "JOIN duplicates usually come from one-to-many matches.",
                List.of(new SourceCitation("doc_sql", "joins.md", 12, "Join cardinality", "One parent row can match multiple child rows.", 0.93)),
                "trc_ai_qa_1"
        ));

        mockMvc.perform(post("/api/ai/qa")
                        .header("X-User-Id", "alice")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Why does SQL JOIN duplicate rows?",
                                  "answerMode": "EXPERT",
                                  "kbIds": ["kb_sql"],
                                  "courseId": "course_sql",
                                  "topK": 5,
                                  "requestId": "req_ai_qa_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.answer").value("JOIN duplicates usually come from one-to-many matches."))
                .andExpect(jsonPath("$.data.answerMode").value("EXPERT"))
                .andExpect(jsonPath("$.data.reasoningEffort").value("high"))
                .andExpect(jsonPath("$.data.reasoningSummary").value(org.hamcrest.Matchers.containsString("EXPERT")))
                .andExpect(jsonPath("$.data.reasoningSummary").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("chain-of-thought"))))
                .andExpect(jsonPath("$.data.sourceStatus").value("COURSE_GROUNDED"))
                .andExpect(jsonPath("$.data.sourcePolicy").value("COURSE_RAG"))
                .andExpect(jsonPath("$.data.traceId").value("trc_ai_qa_1"))
                .andExpect(jsonPath("$.data.workflowId").doesNotExist())
                .andExpect(jsonPath("$.data.toolCalls").isArray())
                .andExpect(jsonPath("$.data.toolCalls[0].name").value("IntentRouter"))
                .andExpect(jsonPath("$.data.toolCalls[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.toolCalls[2].name").value("MemoryContextService"))
                .andExpect(jsonPath("$.data.toolCalls[2].summary").value(org.hamcrest.Matchers.containsString("contextItems=")))
                .andExpect(jsonPath("$.data.citations[0].documentName").value("joins.md"))
                .andExpect(jsonPath("$.data.learnerFit.summary").value(org.hamcrest.Matchers.containsString("profile:")))
                .andExpect(jsonPath("$.data.nextSteps").isArray())
                .andExpect(jsonPath("$.data.nextSteps[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data.uncertainty.level").value("LOW"))
                .andExpect(jsonPath("$.data.qualityFlags").value(org.hamcrest.Matchers.hasItem("STRUCTURED_SCHEMA_V1")))
                .andExpect(jsonPath("$.data.verification.verdict").value("PASS"))
                .andExpect(jsonPath("$.data.verification.gatePolicy").value("BASIC_QA_VERIFIER_V1"))
                .andExpect(jsonPath("$.data.toolCalls[5].name").value("AnswerVerifier"))
                .andExpect(jsonPath("$.data.requiresReview").value(false))
                .andExpect(jsonPath("$.data.sources[0].documentName").value("joins.md"));

        verify(ragQueryService).queryWithRequestId(
                "alice",
                false,
                false,
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "req_ai_qa_1"
        );
    }

    @Test
    void aiQaStreamEndpointEmitsStatusTokenAndDoneWithVerification() throws Exception {
        when(ragQueryService.queryWithRequestId(
                "alice",
                false,
                false,
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "req_ai_qa_stream"
        )).thenReturn(new RagQueryResponse(
                "JOIN duplicates usually come from one-to-many matches.",
                List.of(new SourceCitation("doc_sql", "joins.md", 12, "Join cardinality", "One parent row can match multiple child rows.", 0.93)),
                "trc_ai_qa_stream"
        ));

        var mvcResult = mockMvc.perform(post("/api/ai/qa/stream")
                        .header("X-User-Id", "alice")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Why does SQL JOIN duplicate rows?",
                                  "answerMode": "THINKING",
                                  "kbIds": ["kb_sql"],
                                  "courseId": "course_sql",
                                  "topK": 5,
                                  "requestId": "req_ai_qa_stream"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:status")))
                .andExpect(content().string(containsString("INTENT_ROUTING")))
                .andExpect(content().string(containsString("VERIFYING")))
                .andExpect(content().string(containsString("event:token")))
                .andExpect(content().string(containsString("JOIN duplicates usually come from one-to-many matches.")))
                .andExpect(content().string(containsString("event:done")))
                .andExpect(content().string(containsString("trc_ai_qa_stream")))
                .andExpect(content().string(containsString("verification")))
                .andExpect(content().string(containsString("BASIC_QA_VERIFIER_V1")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("chain-of-thought"))));

        verify(ragQueryService).queryWithRequestId(
                "alice",
                false,
                false,
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "req_ai_qa_stream"
        );
    }

    @Test
    void aiQaEndpointDefaultsModeToThinkingAndUsesBearerRoleFacts() throws Exception {
        when(ragQueryService.query(
                "ops_admin",
                true,
                false,
                List.of("kb_ops"),
                "Explain the lesson.",
                null
        )).thenReturn(new RagQueryResponse(
                "Lesson answer.",
                List.of(),
                "trc_ai_qa_admin"
        ));

        mockMvc.perform(post("/api/ai/qa")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "spoofed_student")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Explain the lesson.",
                                  "kbIds": ["kb_ops"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerMode").value("THINKING"))
                .andExpect(jsonPath("$.data.reasoningEffort").value("medium"))
                .andExpect(jsonPath("$.data.sourceStatus").value("GENERAL_FALLBACK"))
                .andExpect(jsonPath("$.data.sourcePolicy").value("NO_COURSE_SOURCE_FALLBACK"))
                .andExpect(jsonPath("$.data.traceId").value("trc_ai_qa_admin"));

        verify(ragQueryService).query(
                "ops_admin",
                true,
                false,
                List.of("kb_ops"),
                "Explain the lesson.",
                null
        );
    }

    @Test
    void aiQaEndpointFallsBackToGeneralAnswerWhenRagHasNoReliableSources() throws Exception {
        when(ragQueryService.query(
                "alice",
                false,
                false,
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5
        )).thenReturn(new RagQueryResponse(
                "No cited course material was found for the question: Why does SQL JOIN duplicate rows?",
                List.of(),
                "trc_ai_qa_no_source"
        ));

        mockMvc.perform(post("/api/ai/qa")
                        .header("X-User-Id", "alice")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Why does SQL JOIN duplicate rows?",
                                  "answerMode": "EXPERT",
                                  "kbIds": ["kb_sql"],
                                  "topK": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("通用解释")))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("No cited course material"))))
                .andExpect(jsonPath("$.data.sourceStatus").value("GENERAL_FALLBACK"))
                .andExpect(jsonPath("$.data.sourcePolicy").value("NO_COURSE_SOURCE_FALLBACK"))
                .andExpect(jsonPath("$.data.sources").isEmpty())
                .andExpect(jsonPath("$.data.citations").isEmpty())
                .andExpect(jsonPath("$.data.uncertainty.level").value("MEDIUM"))
                .andExpect(jsonPath("$.data.qualityFlags").value(org.hamcrest.Matchers.hasItem("NO_SOURCE_FALLBACK")))
                .andExpect(jsonPath("$.data.verification.verdict").value("PASS"))
                .andExpect(jsonPath("$.data.verification.requiresReview").value(true))
                .andExpect(jsonPath("$.data.requiresReview").value(true))
                .andExpect(jsonPath("$.data.traceId").value("trc_ai_qa_no_source"));

        verify(ragQueryService).query(
                "alice",
                false,
                false,
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5
        );
    }

    @Test
    void aiQaEndpointRejectsUnknownAnswerMode() throws Exception {
        mockMvc.perform(post("/api/ai/qa")
                        .header("X-User-Id", "alice")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Explain the lesson.",
                                  "answerMode": "XHIGH",
                                  "kbIds": ["kb_ops"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
