package com.learningos.agent.application;

import com.learningos.agent.domain.AgentTrace;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.agent.repository.ModelCallLogRepository;
import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.common.observability.LearningOsMetrics;
import com.learningos.common.exception.ApiException;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({AgentRunRecorder.class, AgentRunRecorderTest.MetricsTestConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AgentRunRecorderTest {

    private final AgentRunRecorder recorder;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final ModelCallLogRepository modelCallLogRepository;
    private final TokenUsageLogRepository tokenUsageLogRepository;
    private final SimpleMeterRegistry meterRegistry;
    private final EntityManager entityManager;

    AgentRunRecorderTest(
            AgentRunRecorder recorder,
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            ModelCallLogRepository modelCallLogRepository,
            TokenUsageLogRepository tokenUsageLogRepository,
            SimpleMeterRegistry meterRegistry,
            EntityManager entityManager
    ) {
        this.recorder = recorder;
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.modelCallLogRepository = modelCallLogRepository;
        this.tokenUsageLogRepository = tokenUsageLogRepository;
        this.meterRegistry = meterRegistry;
        this.entityManager = entityManager;
    }

    @Test
    void startsRunAndRecordsTraceModelAndTokenEvidence() {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{\"learnerId\":\"alice\"}",
                "{\"summary\":\"Draft complete\"}",
                "trace_unit",
                123L
        ));

        assertThat(context.agentTaskId()).startsWith("agt_");
        assertThat(context.traceId()).isEqualTo("trace_unit");
        assertThat(agentTaskRepository.findById(context.agentTaskId()))
                .hasValueSatisfying(task -> {
                    assertThat(task.getOwnerUserId()).isEqualTo("alice");
                    assertThat(task.getTaskType()).isEqualTo(AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION);
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_RUNNING);
                    assertThat(task.getInputJson()).contains("alice");
                    assertThat(task.getOutputJson()).contains("Draft complete");
                    assertThat(task.getTraceId()).isEqualTo("trace_unit");
                    assertThat(task.getLatencyMs()).isEqualTo(123L);
                });

        List<AgentTrace> traces = recorder.recordTraceSteps(context, List.of(
                new AgentRunRecorder.TraceStep(
                        "step_planner",
                        "PlannerAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Planned the resource workflow.",
                        42L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1
                ),
                new AgentRunRecorder.TraceStep(
                        "step_critic",
                        "CriticAgent",
                        AgentRuntimeConstants.STATUS_PENDING,
                        "Waiting for critic review.",
                        0L,
                        AgentRuntimeConstants.PROMPT_CRITIC_V1
                )
        ));

        assertThat(traces).hasSize(2);
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(context.agentTaskId()))
                .hasSize(2)
                .satisfiesExactly(
                        trace -> {
                            assertThat(trace.getSequenceNo()).isEqualTo(1);
                            assertThat(trace.getStepId()).isEqualTo("step_planner");
                            assertThat(trace.getModel()).isEqualTo(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
                            assertThat(trace.getPromptVersion()).isEqualTo(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
                        },
                        trace -> {
                            assertThat(trace.getSequenceNo()).isEqualTo(2);
                            assertThat(trace.getStepId()).isEqualTo("step_critic");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_PENDING);
                            assertThat(trace.getPromptVersion()).isEqualTo(AgentRuntimeConstants.PROMPT_CRITIC_V1);
                        }
                );

        recorder.recordSuccessfulModelEvidence(
                context,
                traces.get(0),
                new AgentRunRecorder.TokenUsageEstimate(120, 80, 0.0)
        );

        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getTraceId()).isEqualTo("trace_unit");
                    assertThat(log.getAgentTaskId()).isEqualTo(context.agentTaskId());
                    assertThat(log.getModel()).isEqualTo(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
                    assertThat(log.getProvider()).isEqualTo("none");
                    assertThat(log.getStatus()).isEqualTo(AgentRuntimeConstants.MODEL_CALL_SUCCESS);
                    assertThat(log.getLatencyMs()).isEqualTo(42L);
                    assertThat(log.getEstimatedCost()).isZero();
                    assertThat(log.getPromptCode()).isEqualTo("resource-generation");
                    assertThat(log.getPromptVersion()).isEqualTo(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
                    assertThat(log.getTemperature()).isEqualTo(0.0);
                    assertThat(log.getStructuredOutputSchema())
                            .contains("ResourceGenerationOutputSchema", "resources", "title", "citationSummary");
                });
        assertThat(tokenUsageLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getTraceId()).isEqualTo("trace_unit");
                    assertThat(log.getAgentTaskId()).isEqualTo(context.agentTaskId());
                    assertThat(log.getPromptTokens()).isEqualTo(120);
                    assertThat(log.getCompletionTokens()).isEqualTo(80);
                    assertThat(log.getTotalTokens()).isEqualTo(200);
                    assertThat(log.getEstimatedCost()).isZero();
                });
        assertThat(meterRegistry.find("learningos.token.usage")
                .tag("model", AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO)
                .tag("prompt_code", "resource-generation")
                .tag("token_type", "prompt")
                .summary()
                .totalAmount()).isEqualTo(120.0);
        assertThat(meterRegistry.find("learningos.token.usage")
                .tag("model", AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO)
                .tag("prompt_code", "resource-generation")
                .tag("token_type", "completion")
                .summary()
                .totalAmount()).isEqualTo(80.0);
        assertThat(meterRegistry.find("learningos.token.usage")
                .tag("model", AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO)
                .tag("prompt_code", "resource-generation")
                .tag("token_type", "total")
                .summary()
                .totalAmount()).isEqualTo(200.0);
        assertThat(meterRegistry.find("learningos.token.cost")
                .tag("currency", "USD")
                .summary()
                .count()).isEqualTo(1);
        assertNoSensitiveMetricTags();
    }

    @Test
    void recordsSuccessfulModelEvidenceFromGatewayResponseInsteadOfFirstTrace() throws Exception {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{\"learnerId\":\"alice\"}",
                "{\"summary\":\"Draft complete\"}",
                "trace_gateway_model",
                123L
        ));
        List<AgentTrace> traces = recorder.recordTraceSteps(context, List.of(
                new AgentRunRecorder.TraceStep(
                        "step_resource",
                        "ResourceAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Generated resources.",
                        42L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1
                )
        ));
        AiModelGateway.ModelResponse gatewayResponse = modelResponseWithLatency(
                "openai",
                "gpt-test-resource",
                "raw content with sk-live-secret should not be persisted",
                new AiModelGateway.TokenUsage(123, 45, 168, 0.012),
                321L
        );

        recordSuccessfulModelEvidence(context, traces.get(0), gatewayResponse);

        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getTraceId()).isEqualTo("trace_gateway_model");
                    assertThat(log.getAgentTaskId()).isEqualTo(context.agentTaskId());
                    assertThat(log.getProvider()).isEqualTo("openai");
                    assertThat(log.getModel()).isEqualTo("gpt-test-resource");
                    assertThat(log.getStatus()).isEqualTo(AgentRuntimeConstants.MODEL_CALL_SUCCESS);
                    assertThat(log.getLatencyMs()).isEqualTo(321L);
                    assertThat(log.getEstimatedCost()).isEqualTo(0.012);
                    assertThat(log.getErrorMessage()).isNull();
                    assertThat(log.getPromptCode()).isEqualTo("resource-generation");
                    assertThat(log.getPromptVersion()).isEqualTo(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
                    assertThat(log.getStructuredOutputSchema())
                            .contains("ResourceGenerationOutputSchema", "resources")
                            .doesNotContain("sk-live-secret", "raw content");
                });
        assertThat(tokenUsageLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getPromptTokens()).isEqualTo(123);
                    assertThat(log.getCompletionTokens()).isEqualTo(45);
                    assertThat(log.getTotalTokens()).isEqualTo(168);
                    assertThat(log.getEstimatedCost()).isEqualTo(0.012);
                });
        assertThat(meterRegistry.find("learningos.token.usage")
                .tag("model", "gpt-test-resource")
                .tag("prompt_code", "resource-generation")
                .tag("token_type", "total")
                .summary()
                .totalAmount()).isEqualTo(168.0);
    }

    @Test
    void recordsSafeProviderForGatewayModelEvidence() throws Exception {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{}",
                "{}",
                "trace_sensitive_provider",
                1L
        ));
        List<AgentTrace> traces = recorder.recordTraceSteps(context, List.of(
                new AgentRunRecorder.TraceStep(
                        "step_resource",
                        "ResourceAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Generated resources.",
                        1L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1
                )
        ));

        recordSuccessfulModelEvidence(context, traces.get(0), modelResponseWithLatency(
                "https://provider.example/v1?apiKey=sk-live-secret",
                "gpt-test-sensitive-provider",
                "safe content",
                new AiModelGateway.TokenUsage(1, 1, 2, 0.0),
                1L
        ));

        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getProvider()).isEqualTo("other");
                    assertThat(log.getProvider()).doesNotContain("https://", "apiKey", "sk-live-secret");
                });
    }

    @Test
    void recordsProviderForFailureWithoutLeakingRawProviderError() throws Exception {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{}",
                "{}",
                "trace_provider_failure",
                1L
        ));

        recordFailureWithProvider(
                context,
                "step_resource",
                "ResourceAgent",
                "Model call failed while drafting resources.",
                20L,
                AgentRuntimeConstants.PROMPT_RESOURCE_V1,
                "OPENAI",
                "401 apiKey=sk-live-secret raw prompt"
        );

        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getProvider()).isEqualTo("openai");
                    assertThat(log.getErrorMessage()).isEqualTo("MODEL_PROVIDER_ERROR");
                    assertThat(log.getErrorMessage()).doesNotContain("sk-live-secret", "raw prompt", "401");
                });
    }

    @Test
    void recordsFailureByUpdatingTaskAndWritingTraceAndModelError() {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{\"learnerId\":\"alice\"}",
                "{\"summary\":\"Generation started\"}",
                "trace_failure",
                10L
        ));

        AgentTrace failureTrace = recorder.recordFailure(
                context,
                "step_resource",
                "ResourceAgent",
                "Model call failed while drafting resources.",
                88L,
                AgentRuntimeConstants.PROMPT_RESOURCE_V1,
                "provider timeout"
        );

        assertThat(agentTaskRepository.findById(context.agentTaskId()))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                    assertThat(task.getOutputJson()).contains("Model call failed", "MODEL_PROVIDER_ERROR", "\"recoverable\":true");
                    assertThat(task.getOutputJson()).doesNotContain("provider timeout");
                    assertThat(task.getTraceId()).isEqualTo("trace_failure");
                });
        assertThat(failureTrace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
        assertThat(failureTrace.getSummary()).contains("Model call failed", "MODEL_PROVIDER_ERROR");
        assertThat(failureTrace.getSummary()).doesNotContain("provider timeout");
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(context.agentTaskId()))
                .singleElement()
                .satisfies(trace -> {
                    assertThat(trace.getStepId()).isEqualTo("step_resource");
                    assertThat(trace.getSequenceNo()).isEqualTo(1);
                    assertThat(trace.getPromptVersion()).isEqualTo(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
                });
        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getProvider()).isEqualTo("none");
                    assertThat(log.getStatus()).isEqualTo(AgentRuntimeConstants.MODEL_CALL_FAILED);
                    assertThat(log.getErrorMessage()).isEqualTo("MODEL_PROVIDER_ERROR");
                    assertThat(log.getLatencyMs()).isEqualTo(88L);
                    assertThat(log.getPromptCode()).isEqualTo("resource-generation");
                    assertThat(log.getPromptVersion()).isEqualTo(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
                    assertThat(log.getTemperature()).isEqualTo(0.0);
                    assertThat(log.getStructuredOutputSchema())
                            .contains("ResourceGenerationOutputSchema", "resources", "title", "citationSummary");
                });
    }

    @Test
    void appendsTraceStepsAfterExistingStepsWithContinuousSequenceNumbers() {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{}",
                "{}",
                "trace_append",
                1L
        ));

        recorder.recordTraceSteps(context, List.of(
                new AgentRunRecorder.TraceStep(
                        "workflow_start",
                        "Orchestrator",
                        AgentRuntimeConstants.STATUS_RUNNING,
                        "Workflow started.",
                        0L,
                        null
                )
        ));
        recorder.recordTraceSteps(context, List.of(
                new AgentRunRecorder.TraceStep(
                        "step_planner",
                        "PlannerAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Planned resources.",
                        10L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1
                ),
                new AgentRunRecorder.TraceStep(
                        "step_resource",
                        "ResourceAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Generated resources.",
                        20L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1
                )
        ));

        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(context.agentTaskId()))
                .hasSize(3)
                .satisfiesExactly(
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("workflow_start");
                            assertThat(trace.getSequenceNo()).isEqualTo(1);
                        },
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("step_planner");
                            assertThat(trace.getSequenceNo()).isEqualTo(2);
                        },
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("step_resource");
                            assertThat(trace.getSequenceNo()).isEqualTo(3);
                        }
                );
    }

    @Test
    void rejectsInvalidTaskAndTraceStatusesBeforePersisting() {
        assertThatThrownBy(() -> recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                "BROKEN_STATUS",
                "{}",
                "{}",
                "trace_invalid_task",
                1L
        ))).isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid agent task status");
        assertThat(agentTaskRepository.count()).isZero();

        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{}",
                "{}",
                "trace_invalid_step",
                1L
        ));

        assertThatThrownBy(() -> recorder.recordTraceSteps(context, List.of(
                new AgentRunRecorder.TraceStep(
                        "step_unknown",
                        "UnknownAgent",
                        "BROKEN_STEP_STATUS",
                        "Should not be persisted.",
                        1L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1
                )
        ))).isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid agent trace status");
        assertThat(agentTraceRepository.countByAgentTaskId(context.agentTaskId())).isZero();
    }

    @Test
    void cancelsRunningTaskAndAppendsCancellationTrace() {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{\"learnerId\":\"alice\"}",
                "{\"summary\":\"Generation started\"}",
                "trace_cancel",
                10L
        ));

        AgentTrace cancellationTrace = recorder.cancelTask(
                "alice",
                context.agentTaskId(),
                "User cancelled duplicated generation."
        );

        assertThat(agentTaskRepository.findById(context.agentTaskId()))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_CANCELLED);
                    assertThat(task.getOutputJson()).contains("CANCELLED", "User cancelled duplicated generation.");
                });
        assertThat(cancellationTrace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_CANCELLED);
        assertThat(cancellationTrace.getSummary()).contains("User cancelled duplicated generation.");
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(context.agentTaskId()))
                .singleElement()
                .satisfies(trace -> {
                    assertThat(trace.getStepId()).isEqualTo("task_cancelled");
                    assertThat(trace.getSequenceNo()).isEqualTo(1);
                });
    }

    @Test
    void rejectsCancellationAfterTerminalStatus() {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{}",
                "{}",
                "trace_terminal_cancel",
                1L
        ));
        recorder.recordFailure(
                context,
                "step_resource",
                "ResourceAgent",
                "Model failed.",
                1L,
                AgentRuntimeConstants.PROMPT_RESOURCE_V1,
                "timeout"
        );

        assertThatThrownBy(() -> recorder.cancelTask("alice", context.agentTaskId(), "Too late."))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Terminal agent task cannot be cancelled");
    }

    @Test
    void recordsToolCallWithSanitizedSummariesAndRetentionClass() throws Exception {
        AgentExecutionContext context = recorder.startRun(new AgentRunRecorder.RunStart(
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                "{}",
                "{}",
                "trace_tool_call",
                1L
        ));

        recordToolCall(
                context,
                "CourseRagTool",
                "{\"query\":\"JOIN\",\"secret\":\"sk-live-secret\",\"privateDocument\":\"full private course document\"}",
                "{\"matches\":3,\"token\":\"raw-token\",\"fullText\":\"full generated private output\"}",
                AgentRuntimeConstants.STATUS_FAILED,
                "Tool failed with password=plain-secret",
                35L
        );
        entityManager.flush();

        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        select tool_name,
                               input_json,
                               output_json,
                               status,
                               error_message,
                               latency_ms,
                               trace_id,
                               input_summary,
                               output_summary,
                               retention_class
                        from agent_tool_call
                        where agent_task_id = ?1
                        """)
                .setParameter(1, context.agentTaskId())
                .getSingleResult();

        assertThat(row[0]).isEqualTo("CourseRagTool");
        assertThat((String) row[1]).contains("[REDACTED]", "[REDACTED_DOCUMENT]");
        assertThat((String) row[1]).doesNotContain("sk-live-secret", "full private course document");
        assertThat((String) row[2]).contains("[REDACTED]", "[REDACTED_DOCUMENT]");
        assertThat((String) row[2]).doesNotContain("raw-token", "full generated private output");
        assertThat(row[3]).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
        assertThat(row[4]).isEqualTo("TOOL_CALL_FAILED");
        assertThat(((Number) row[5]).longValue()).isEqualTo(35L);
        assertThat(row[6]).isEqualTo("trace_tool_call");
        assertThat((String) row[7]).contains("[REDACTED]", "[REDACTED_DOCUMENT]");
        assertThat((String) row[8]).contains("[REDACTED]", "[REDACTED_DOCUMENT]");
        assertThat(row[9]).isEqualTo("SANITIZED_SUMMARY");
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

    private void recordSuccessfulModelEvidence(
            AgentExecutionContext context,
            AgentTrace trace,
            AiModelGateway.ModelResponse modelResponse
    ) throws Exception {
        Method method = recorder.getClass().getMethod(
                "recordSuccessfulModelEvidence",
                AgentExecutionContext.class,
                AgentTrace.class,
                AiModelGateway.ModelResponse.class
        );
        try {
            method.invoke(recorder, context, trace, modelResponse);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw ex;
        }
    }

    private void recordFailureWithProvider(
            AgentExecutionContext context,
            String stepId,
            String agentName,
            String summary,
            Long latencyMs,
            String promptVersion,
            String provider,
            String errorMessage
    ) throws Exception {
        Method method = recorder.getClass().getMethod(
                "recordFailure",
                AgentExecutionContext.class,
                String.class,
                String.class,
                String.class,
                Long.class,
                String.class,
                String.class,
                String.class
        );
        try {
            method.invoke(recorder, context, stepId, agentName, summary, latencyMs, promptVersion, provider, errorMessage);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw ex;
        }
    }

    private AiModelGateway.ModelResponse modelResponseWithLatency(
            String provider,
            String model,
            String content,
            AiModelGateway.TokenUsage tokenUsage,
            long latencyMs
    ) throws Exception {
        try {
            return AiModelGateway.ModelResponse.class
                    .getConstructor(
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            Map.class,
                            AiModelGateway.TokenUsage.class,
                            long.class
                    )
                    .newInstance(
                            provider,
                            model,
                            AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                            content,
                            validStructuredOutput(),
                            tokenUsage,
                            latencyMs
                    );
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("ModelResponse should expose gateway latencyMs", exception);
        }
    }

    private Map<String, Object> validStructuredOutput() {
        return Map.of("resources", List.of(Map.of(
                "title", "SQL JOIN remediation",
                "type", "LECTURE",
                "modality", "MARKDOWN",
                "markdownContent", "### SQL JOIN remediation",
                "citationSummary", "COURSE_RAG: 1 persisted source citation.",
                "safetyStatus", AgentRuntimeConstants.SAFETY_NEEDS_REVIEW
        )));
    }

    private void assertNoSensitiveMetricTags() {
        assertThat(meterRegistry.getMeters().stream()
                .map(Meter::getId)
                .flatMap(id -> id.getTags().stream())
                .map(tag -> tag.getKey())
                .toList())
                .doesNotContain("traceId", "userId", "requestId", "agentTaskId", "taskId", "question",
                        "prompt", "source", "errorMessage");
    }

    @TestConfiguration
    static class MetricsTestConfig {

        @Bean
        SimpleMeterRegistry simpleMeterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        LearningOsMetrics learningOsMetrics(SimpleMeterRegistry meterRegistry) {
            return new LearningOsMetrics(meterRegistry);
        }
    }
}
