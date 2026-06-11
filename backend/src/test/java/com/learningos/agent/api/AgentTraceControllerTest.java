package com.learningos.agent.api;

import com.learningos.agent.application.AgentExecutionContext;
import com.learningos.agent.application.AgentRunRecorder;
import com.learningos.agent.application.AgentRuntimeConstants;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.agent.repository.ModelCallLogRepository;
import com.learningos.agent.repository.TokenUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
        "learning-os.auth.issuer=learning-os"
})
class AgentTraceControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final AgentRunRecorder recorder;

    AgentTraceControllerTest(
            MockMvc mockMvc,
            AgentRunRecorder recorder,
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            ModelCallLogRepository modelCallLogRepository,
            TokenUsageLogRepository tokenUsageLogRepository
    ) {
        this.mockMvc = mockMvc;
        this.recorder = recorder;
    }

    @Test
    void searchesTraceGovernanceListByUserAgentStatusTimeAndFailureReason() throws Exception {
        AgentExecutionContext failedContext = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{\"learnerId\":\"alice\"}",
                "{\"summary\":\"Generation started\"}",
                "trace_governance_failed",
                10L
        ));
        recorder.recordFailure(
                failedContext,
                "step_resource",
                "ResourceAgent",
                "Model call failed while drafting resources.",
                88L,
                AgentRuntimeConstants.PROMPT_RESOURCE_V1,
                "provider timeout with token sk-secret"
        );

        AgentExecutionContext doneContext = recorder.startRun(new AgentRunRecorder.RunStart(
                "bob",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{\"learnerId\":\"bob\"}",
                "{\"summary\":\"Generation started\"}",
                "trace_governance_done",
                5L
        ));
        recorder.recordTraceSteps(doneContext, List.of(new AgentRunRecorder.TraceStep(
                "step_resource",
                "ResourceAgent",
                AgentRuntimeConstants.STATUS_DONE,
                "Generated resources.",
                30L,
                AgentRuntimeConstants.PROMPT_RESOURCE_V1
        )));
        recorder.transitionTask(doneContext, AgentRuntimeConstants.STATUS_DONE, "Generated resources.", 30L);

        mockMvc.perform(get("/api/agent/traces")
                        .header("X-User-Id", "admin")
                        .param("userId", "alice")
                        .param("agentType", AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION)
                        .param("status", AgentRuntimeConstants.STATUS_FAILED)
                        .param("from", Instant.now().minusSeconds(60).toString())
                        .param("to", Instant.now().plusSeconds(60).toString())
                        .param("failureReason", "MODEL_PROVIDER_ERROR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].taskId").value(failedContext.agentTaskId()))
                .andExpect(jsonPath("$.data.items[0].traceId").value("trace_governance_failed"))
                .andExpect(jsonPath("$.data.items[0].userId").value("alice"))
                .andExpect(jsonPath("$.data.items[0].agentType").value(AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION))
                .andExpect(jsonPath("$.data.items[0].status").value(AgentRuntimeConstants.STATUS_FAILED))
                .andExpect(jsonPath("$.data.items[0].failureReason").value("MODEL_PROVIDER_ERROR"))
                .andExpect(jsonPath("$.data.items[0].stepCount").value(1));
    }

    @Test
    void traceGovernanceSearchUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        AgentExecutionContext context = seedTrace("alice", "trace_roles_first_admin_search");

        mockMvc.perform(get("/api/agent/traces")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].taskId").value(context.agentTaskId()))
                .andExpect(jsonPath("$.data.items[0].traceId").value("trace_roles_first_admin_search"))
                .andExpect(jsonPath("$.data.items[0].userId").value("alice"));
    }

    @Test
    void traceGovernanceSearchRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        AgentExecutionContext context = seedTrace("alice", "trace_roles_first_subject_admin_search");

        String responseBody = mockMvc.perform(get("/api/agent/traces")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(context.agentTaskId(), "trace_roles_first_subject_admin_search");
    }

    @Test
    void traceDetailExposesSanitizedToolCallsAndRetentionPolicy() throws Exception {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{\"learnerId\":\"alice\"}",
                "{\"summary\":\"Generation started\"}",
                "trace_governance_tool",
                10L
        ));
        recorder.recordTraceSteps(context, List.of(new AgentRunRecorder.TraceStep(
                "step_tool",
                "CourseRagAgent",
                AgentRuntimeConstants.STATUS_DONE,
                "Course RAG tool finished.",
                35L,
                AgentRuntimeConstants.PROMPT_RESOURCE_V1
        )));
        recordToolCall(
                context,
                "CourseRagTool",
                "{\"query\":\"JOIN\",\"apiKey\":\"sk-live-secret\",\"privateDocument\":\"full private course document\"}",
                "{\"matches\":3,\"token\":\"raw-token\",\"rawDocument\":\"raw private output\"}",
                AgentRuntimeConstants.STATUS_DONE,
                null,
                35L
        );

        mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", context.agentTaskId())
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(context.agentTaskId()))
                .andExpect(jsonPath("$.data.toolCalls.length()").value(1))
                .andExpect(jsonPath("$.data.toolCalls[0].toolName").value("CourseRagTool"))
                .andExpect(jsonPath("$.data.toolCalls[0].status").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.toolCalls[0].inputSummary").value(containsString("[REDACTED]")))
                .andExpect(jsonPath("$.data.toolCalls[0].inputSummary").value(containsString("[REDACTED_DOCUMENT]")))
                .andExpect(jsonPath("$.data.toolCalls[0].inputSummary").value(not(containsString("sk-live-secret"))))
                .andExpect(jsonPath("$.data.toolCalls[0].outputSummary").value(containsString("[REDACTED]")))
                .andExpect(jsonPath("$.data.toolCalls[0].outputSummary").value(containsString("[REDACTED_DOCUMENT]")))
                .andExpect(jsonPath("$.data.toolCalls[0].outputSummary").value(not(containsString("raw-token"))))
                .andExpect(jsonPath("$.data.retentionPolicy.auditRetentionDays").value(365))
                .andExpect(jsonPath("$.data.retentionPolicy.textRetentionDays").value(30))
                .andExpect(jsonPath("$.data.retentionPolicy.longTermAuditFields[0]").value("taskId"))
                .andExpect(jsonPath("$.data.retentionPolicy.cleanableTextFields[0]").value("agent_task.inputJson"));
    }

    @Test
    void traceDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        AgentExecutionContext context = seedTrace("alice", "trace_roles_first_admin_detail");

        mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", context.agentTaskId())
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(context.agentTaskId()))
                .andExpect(jsonPath("$.data.traceId").value("trace_roles_first_admin_detail"));
    }

    @Test
    void traceDetailRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        AgentExecutionContext context = seedTrace("alice", "trace_roles_first_subject_admin_detail");

        String responseBody = mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", context.agentTaskId())
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(context.agentTaskId(), "trace_roles_first_subject_admin_detail");
    }

    @Test
    void traceDetailBearerAdminMissingTaskReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", "agt_missing_roles_first")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private AgentExecutionContext seedTrace(String ownerUserId, String traceId) {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                ownerUserId,
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{\"learnerId\":\"" + ownerUserId + "\"}",
                "{\"summary\":\"Generation started\"}",
                traceId,
                10L
        ));
        recorder.recordTraceSteps(context, List.of(new AgentRunRecorder.TraceStep(
                "step_resource",
                "ResourceAgent",
                AgentRuntimeConstants.STATUS_DONE,
                "Generated resources.",
                30L,
                AgentRuntimeConstants.PROMPT_RESOURCE_V1
        )));
        return context;
    }

    private void recordToolCall(
            AgentExecutionContext context,
            String toolName,
            String inputJson,
            String outputJson,
            String status,
            String errorMessage,
            Long latencyMs
    ) throws Exception {
        Method method = recorder.getClass().getMethod(
                "recordToolCall",
                AgentExecutionContext.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Long.class
        );
        try {
            method.invoke(recorder, context, toolName, inputJson, outputJson, status, errorMessage, latencyMs);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw ex;
        }
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
