package com.learningos.common.auth;

import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.application.RagQueryService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SseProductionAuthStrategyTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @NestedTestConfiguration(OVERRIDE)
    @TestPropertySource(properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "learning-os.app.environment=production",
            "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
            "learning-os.auth.issuer=learning-os"
    })
    class Production {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private RagQueryService ragQueryService;

        @Test
        void chatStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork() throws Exception {
            mockMvc.perform(get("/api/chat/sessions/session_1/stream")
                            .header(DevAuthFilter.USER_ID_HEADER, "admin")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(ragQueryService);
        }

        @Test
        void chatStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork() throws Exception {
            mockMvc.perform(get("/api/chat/sessions/session_1/stream")
                            .header("Authorization", "Bearer not-a-jwt")
                            .header(DevAuthFilter.USER_ID_HEADER, "admin")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(ragQueryService);
        }

        @Test
        void chatStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader() throws Exception {
            when(ragQueryService.query("ops_admin", true, false, List.of("kb_foreign"), "What is hidden?", null))
                    .thenReturn(response("Admin-visible answer.", "trc_admin_stream"));

            var mvcResult = mockMvc.perform(get("/api/chat/sessions/session_1/stream")
                            .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                            .header(DevAuthFilter.USER_ID_HEADER, "student_spoof")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            mvcResult.getAsyncResult(5_000L);

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("event:done")))
                    .andExpect(content().string(containsString("trc_admin_stream")));

            verify(ragQueryService).query("ops_admin", true, false, List.of("kb_foreign"), "What is hidden?", null);
        }

        @Test
        void chatStreamInProductionDoesNotInferAdminFromBearerSubjectName() throws Exception {
            when(ragQueryService.query("admin", false, false, List.of("kb_foreign"), "What is hidden?", null))
                    .thenReturn(response("Non-admin role facts only.", "trc_subject_admin_stream"));

            var mvcResult = mockMvc.perform(get("/api/chat/sessions/session_1/stream")
                            .header("Authorization", "Bearer " + jwt("admin", "Literal Admin", List.of("USER")))
                            .header(DevAuthFilter.USER_ID_HEADER, "ops_admin")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            mvcResult.getAsyncResult(5_000L);

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("event:done")))
                    .andExpect(content().string(containsString("trc_subject_admin_stream")));

            verify(ragQueryService).query("admin", false, false, List.of("kb_foreign"), "What is hidden?", null);
        }

        @Test
        void tutorStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork() throws Exception {
            mockMvc.perform(get("/api/tutor/sessions/session_1/stream")
                            .header(DevAuthFilter.USER_ID_HEADER, "admin")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(ragQueryService);
        }

        @Test
        void tutorStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork() throws Exception {
            mockMvc.perform(get("/api/tutor/sessions/session_1/stream")
                            .header("Authorization", "Bearer not-a-jwt")
                            .header(DevAuthFilter.USER_ID_HEADER, "admin")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(ragQueryService);
        }

        @Test
        void tutorStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader() throws Exception {
            when(ragQueryService.query("ops_admin", true, false, List.of("kb_foreign"), "What is hidden?", null))
                    .thenReturn(response("Admin-visible tutor answer.", "trc_tutor_admin_stream"));

            var mvcResult = mockMvc.perform(get("/api/tutor/sessions/session_1/stream")
                            .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                            .header(DevAuthFilter.USER_ID_HEADER, "student_spoof")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            mvcResult.getAsyncResult(5_000L);

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("event:done")))
                    .andExpect(content().string(containsString("trc_tutor_admin_stream")));

            verify(ragQueryService).query("ops_admin", true, false, List.of("kb_foreign"), "What is hidden?", null);
        }

        @Test
        void tutorStreamInProductionDoesNotInferAdminFromBearerSubjectName() throws Exception {
            when(ragQueryService.query("admin", false, false, List.of("kb_foreign"), "What is hidden?", null))
                    .thenReturn(response("Non-admin tutor role facts only.", "trc_tutor_subject_admin_stream"));

            var mvcResult = mockMvc.perform(get("/api/tutor/sessions/session_1/stream")
                            .header("Authorization", "Bearer " + jwt("admin", "Literal Admin", List.of("USER")))
                            .header(DevAuthFilter.USER_ID_HEADER, "ops_admin")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            mvcResult.getAsyncResult(5_000L);

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("event:done")))
                    .andExpect(content().string(containsString("trc_tutor_subject_admin_stream")));

            verify(ragQueryService).query("admin", false, false, List.of("kb_foreign"), "What is hidden?", null);
        }

        @Test
        void postRagStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork() throws Exception {
            mockMvc.perform(post("/api/rag/query/stream")
                            .header(DevAuthFilter.USER_ID_HEADER, "admin")
                            .accept(MediaType.TEXT_EVENT_STREAM)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "question": "What is hidden?",
                                      "kbIds": ["kb_foreign"],
                                      "topK": 5
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(ragQueryService);
        }

        @Test
        void postRagStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork() throws Exception {
            mockMvc.perform(post("/api/rag/query/stream")
                            .header("Authorization", "Bearer not-a-jwt")
                            .header(DevAuthFilter.USER_ID_HEADER, "admin")
                            .accept(MediaType.TEXT_EVENT_STREAM)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "question": "What is hidden?",
                                      "kbIds": ["kb_foreign"],
                                      "topK": 5
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(ragQueryService);
        }

        @Test
        void postRagStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader() throws Exception {
            when(ragQueryService.queryWithRequestId(
                    "ops_admin",
                    true,
                    false,
                    List.of("kb_foreign"),
                    "What is hidden?",
                    5,
                    "req_prod_stream"
            )).thenReturn(response("Admin-visible production stream answer.", "trc_prod_post_stream"));

            var mvcResult = mockMvc.perform(post("/api/rag/query/stream")
                            .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                            .header(DevAuthFilter.USER_ID_HEADER, "student_spoof")
                            .accept(MediaType.TEXT_EVENT_STREAM)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "question": "What is hidden?",
                                      "kbIds": ["kb_foreign"],
                                      "topK": 5,
                                      "requestId": "req_prod_stream"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            mvcResult.getAsyncResult(5_000L);

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("event:done")))
                    .andExpect(content().string(containsString("trc_prod_post_stream")));

            verify(ragQueryService).queryWithRequestId(
                    "ops_admin",
                    true,
                    false,
                    List.of("kb_foreign"),
                    "What is hidden?",
                    5,
                    "req_prod_stream"
            );
        }

        @Test
        void postRagStreamInProductionDoesNotInferAdminFromBearerSubjectName() throws Exception {
            when(ragQueryService.query(
                    "admin",
                    false,
                    false,
                    List.of("kb_foreign"),
                    "What is hidden?",
                    5
            )).thenReturn(response("Non-admin production stream answer.", "trc_prod_subject_admin_stream"));

            var mvcResult = mockMvc.perform(post("/api/rag/query/stream")
                            .header("Authorization", "Bearer " + jwt("admin", "Literal Admin", List.of("USER")))
                            .header(DevAuthFilter.USER_ID_HEADER, "ops_admin")
                            .accept(MediaType.TEXT_EVENT_STREAM)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "question": "What is hidden?",
                                      "kbIds": ["kb_foreign"],
                                      "topK": 5
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            mvcResult.getAsyncResult(5_000L);

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("event:done")))
                    .andExpect(content().string(containsString("trc_prod_subject_admin_stream")));

            verify(ragQueryService).query(
                    "admin",
                    false,
                    false,
                    List.of("kb_foreign"),
                    "What is hidden?",
                    5
            );
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @NestedTestConfiguration(OVERRIDE)
    @TestPropertySource(properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "learning-os.app.environment=staging",
            "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
            "learning-os.auth.issuer=learning-os"
    })
    class Staging {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private RagQueryService ragQueryService;

        @Test
        void chatStreamInStagingRejectsHeaderOnlyAuthBeforeStartingAsyncWork() throws Exception {
            mockMvc.perform(get("/api/chat/sessions/session_1/stream")
                            .header(DevAuthFilter.USER_ID_HEADER, "admin")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(ragQueryService);
        }

        @Test
        void tutorStreamInStagingRejectsHeaderOnlyAuthBeforeStartingAsyncWork() throws Exception {
            mockMvc.perform(get("/api/tutor/sessions/session_1/stream")
                            .header(DevAuthFilter.USER_ID_HEADER, "admin")
                            .param("question", "What is hidden?")
                            .param("kbIds", "kb_foreign"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(request().asyncNotStarted())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(ragQueryService);
        }
    }

    private static RagQueryResponse response(String answer, String traceId) {
        return new RagQueryResponse(
                answer,
                List.of(new SourceCitation("doc_hidden", "hidden.md", 2, "Hidden", "Hidden material", 1.0)),
                traceId
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
