package com.learningos.tutor.api;

import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.application.RagQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class TutorControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;

    @MockBean
    private RagQueryService ragQueryService;

    TutorControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void askDelegatesToRagQueryServiceAndReturnsApiEnvelope() throws Exception {
        when(ragQueryService.query("alice", false, false, List.of("kb_sql"), "Why do joins duplicate rows?", 3))
                .thenReturn(new RagQueryResponse(
                        "JOIN duplicates come from one-to-many relationships.",
                        List.of(new SourceCitation("doc_sql", "joins.md", 12, "Joins", "JOIN duplicates", 1.0)),
                        "trc_tutor"
                ));

        mockMvc.perform(post("/api/tutor/ask")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Why do joins duplicate rows?",
                                  "kbIds": ["kb_sql"],
                                  "topK": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.answer").value("JOIN duplicates come from one-to-many relationships."))
                .andExpect(jsonPath("$.data.sources.length()").value(1))
                .andExpect(jsonPath("$.data.traceId").value("trc_tutor"));

        verify(ragQueryService).query("alice", false, false, List.of("kb_sql"), "Why do joins duplicate rows?", 3);
    }

    @Test
    void askUsesBearerAdminRoleFactsAndIgnoresSpoofedUserIdHeader() throws Exception {
        when(ragQueryService.query("ops_admin", true, false, List.of("kb_foreign"), "What is hidden?", 3))
                .thenReturn(new RagQueryResponse(
                        "Hidden course material.",
                        List.of(new SourceCitation("doc_hidden", "hidden.md", 2, "Hidden", "Hidden material", 1.0)),
                        "trc_tutor_admin"
                ));

        mockMvc.perform(post("/api/tutor/ask")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "student_spoof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What is hidden?",
                                  "kbIds": ["kb_foreign"],
                                  "topK": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.traceId").value("trc_tutor_admin"));

        verify(ragQueryService).query("ops_admin", true, false, List.of("kb_foreign"), "What is hidden?", 3);
    }

    @Test
    void streamDelegatesToRagQueryServiceAndEmitsTutorEvents() throws Exception {
        when(ragQueryService.query("alice", false, false, List.of("kb_sql"), "What is JOIN?", null))
                .thenReturn(new RagQueryResponse(
                        "JOIN duplicates come from one-to-many relationships.",
                        List.of(new SourceCitation("doc_sql", "joins.md", 12, "Joins", "JOIN duplicates", 1.0)),
                        "trc_tutor_stream"
                ));

        var mvcResult = mockMvc.perform(get("/api/tutor/sessions/session_1/stream")
                        .header("X-User-Id", "alice")
                        .param("question", "What is JOIN?")
                        .param("kbIds", "kb_sql"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("event:status")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("event:token")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("JOIN duplicates come from one-to-many relationships.")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("trc_tutor_stream")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("event:done")));

        verify(ragQueryService).query("alice", false, false, List.of("kb_sql"), "What is JOIN?", null);
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
