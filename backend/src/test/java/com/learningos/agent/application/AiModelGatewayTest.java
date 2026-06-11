package com.learningos.agent.application;

import com.learningos.config.AiModelProperties;
import com.learningos.common.observability.LearningOsMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AiModelGatewayTest {

    @Test
    void returnsDeterministicStructuredOutputWhenProviderIsDisabled() {
        AiModelGateway gateway = new AiModelGateway(new AiModelProperties("none", "", ""), LearningOsMetrics.noop());

        AiModelGateway.ModelResponse response = gateway.generateStructured(new AiModelGateway.ModelRequest(
                "resource-generator",
                "Generate a short SQL JOIN remediation resource.",
                Map.of("learnerId", "alice", "pathNodeId", "node_sql_join")
        ));

        assertThat(response.provider()).isEqualTo("none");
        assertThat(response.model()).isEqualTo(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
        assertThat(response.status()).isEqualTo(AgentRuntimeConstants.MODEL_CALL_SUCCESS);
        assertThat(response.content()).contains("resource-generator");
        assertThat(response.structuredOutput()).containsEntry("mode", "deterministic");
        assertThat(response.structuredOutput()).containsEntry("agentName", "resource-generator");
        assertThat(response.tokenUsage().promptTokens()).isPositive();
        assertThat(response.tokenUsage().totalTokens()).isGreaterThanOrEqualTo(response.tokenUsage().promptTokens());
    }

    @Test
    void usesDeterministicFallbackWhenTokenBudgetGateBlocksExternalCalls() {
        TokenBudgetGateService gateService = mock(TokenBudgetGateService.class);
        when(gateService.evaluate()).thenReturn(new TokenBudgetGateService.TokenBudgetDecision(
                true,
                12_000L,
                10_000L,
                0L,
                TokenBudgetGateService.FALLBACK_DETERMINISTIC_ONLY
        ));
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                LearningOsMetrics.noop(),
                mock(ChatModel.class),
                new ObjectMapper(),
                null,
                null,
                gateService
        );

        AiModelGateway.ModelResponse response = gateway.generateStructured(new AiModelGateway.ModelRequest(
                "resource-generator",
                "Generate resource",
                Map.of()
        ));

        assertThat(response.model()).isEqualTo(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
        assertThat(response.structuredOutput()).containsEntry("mode", "deterministic");
        assertThat(response.structuredOutput()).containsEntry("budgetGateReason", "token_budget_gate");
    }

    @Test
    void configuredProviderWithoutChatModelBeanFailsClosedOnDirectCall() {
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                LearningOsMetrics.noop()
        );

        assertThatThrownBy(() -> gateway.generateStructured(new AiModelGateway.ModelRequest(
                "critic-agent",
                "Review resource grounding.",
                Map.of()
        )))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("MODEL_PROVIDER_ERROR");
    }

    @Test
    void usesRegistryDefaultProviderBeforeEnvironmentChatModel() {
        ChatModel chatModel = prompt -> new ChatResponse(
                List.of(new Generation(new AssistantMessage("""
                        {
                          "resources": [
                            {
                              "title": "Registry resource",
                              "type": "LECTURE",
                              "modality": "MARKDOWN",
                              "markdownContent": "### Registry resource",
                              "citationSummary": "COURSE_RAG: chunk_registry",
                              "safetyStatus": "NEEDS_REVIEW"
                            }
                          ]
                        }
                        """))),
                ChatResponseMetadata.builder()
                        .model("deepseek-chat")
                        .usage(new FixedUsage(12, 8))
                        .build()
        );
        ModelProviderService modelProviderService = mock(ModelProviderService.class);
        when(modelProviderService.resolveDefaultChatProvider()).thenReturn(Optional.of(
                new ModelProviderService.ResolvedChatProvider(
                        "deepseek",
                        "deepseek-chat",
                        "https://api.deepseek.com",
                        "sk-registry-key"
                )
        ));
        ChatModelFactory factory = mock(ChatModelFactory.class);
        when(factory.createChatModel(any(), any(), any())).thenReturn(chatModel);
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("none", "", ""),
                LearningOsMetrics.noop(),
                null,
                new ObjectMapper(),
                modelProviderService,
                factory
        );

        AiModelGateway.ModelResponse response = gateway.generateStructured(new AiModelGateway.ModelRequest(
                "resource-generator",
                "Generate a registry-backed resource.",
                Map.of("learnerId", "alice"),
                AgentRuntimeConstants.PROMPT_RESOURCE_V1
        ));

        assertThat(response.provider()).isEqualTo("deepseek");
        assertThat(response.model()).isEqualTo("deepseek-chat");
        assertThat(response.status()).isEqualTo(AgentRuntimeConstants.MODEL_CALL_SUCCESS);
    }

    @Test
    void callsRealChatModelAdapterWhenProviderAndChatModelAreConfigured() {
        ChatModel chatModel = prompt -> new ChatResponse(
                List.of(new Generation(new AssistantMessage("""
                        {
                          "resources": [
                            {
                              "title": "SQL JOIN remediation",
                              "type": "LECTURE",
                              "modality": "MARKDOWN",
                              "markdownContent": "### SQL JOIN remediation\\nCheck join cardinality before aggregating.",
                              "citationSummary": "COURSE_RAG: chunk_1",
                              "safetyStatus": "NEEDS_REVIEW"
                            }
                          ]
                        }
                        """))),
                ChatResponseMetadata.builder()
                        .model("gpt-provider-returned")
                        .usage(new FixedUsage(31, 17))
                        .build()
        );
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-configured", "embed-test"),
                LearningOsMetrics.noop(),
                chatModel
        );

        AiModelGateway.ModelResponse response = gateway.generateStructured(new AiModelGateway.ModelRequest(
                "resource-generator",
                "Generate a short SQL JOIN remediation resource.",
                Map.of("learnerId", "alice", "pathNodeId", "node_sql_join"),
                AgentRuntimeConstants.PROMPT_RESOURCE_V1
        ));

        assertThat(response.provider()).isEqualTo("openai");
        assertThat(response.model()).isEqualTo("gpt-provider-returned");
        assertThat(response.content()).contains("SQL JOIN remediation");
        assertThat(response.structuredOutput()).containsKey("resources");
        assertThat(response.structuredOutput()).doesNotContainEntry("mode", "gateway-placeholder");
        assertThat(response.tokenUsage().promptTokens()).isEqualTo(31);
        assertThat(response.tokenUsage().completionTokens()).isEqualTo(17);
        assertThat(response.tokenUsage().totalTokens()).isEqualTo(48);
    }

    @Test
    void failsClosedWhenConfiguredProviderHasNoChatModelBean() {
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                LearningOsMetrics.noop()
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);
        AgentExecutionContext context = new AgentExecutionContext("agt_no_chat_bean", "trace_no_chat_bean");

        assertThatThrownBy(() -> gateway.generateStructuredWithRetry(
                modelRequest(),
                context,
                recorder,
                failureTrace()
        ))
                .isInstanceOf(AiModelGateway.ModelCallFailedException.class)
                .hasMessage("MODEL_PROVIDER_ERROR");

        verify(recorder).recordFailure(
                eq(context),
                eq("step_resource"),
                eq("ResourceAgent"),
                eq("Model call failed while drafting resources."),
                anyLong(),
                eq(AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                eq("openai"),
                eq("MODEL_PROVIDER_ERROR")
        );
    }

    @Test
    void mapsNonJsonProviderOutputToStructuredOutputInvalid() {
        ChatModel chatModel = prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(
                "not json and should never be exposed"
        ))));
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                LearningOsMetrics.noop(),
                chatModel
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);
        AgentExecutionContext context = new AgentExecutionContext("agt_bad_json", "trace_bad_json");

        assertThatThrownBy(() -> gateway.generateStructuredWithRetry(
                modelRequest(),
                context,
                recorder,
                failureTrace()
        ))
                .isInstanceOf(AiModelGateway.ModelCallFailedException.class)
                .hasMessage("STRUCTURED_OUTPUT_INVALID")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("not json"));

        verify(recorder).recordFailure(
                eq(context),
                eq("step_resource"),
                eq("ResourceAgent"),
                eq("Model call failed while drafting resources."),
                anyLong(),
                eq(AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                eq("openai"),
                eq("STRUCTURED_OUTPUT_INVALID")
        );
    }

    @Test
    void sanitizesRawChatProviderErrorFromAdapterFailure() {
        ChatModel chatModel = prompt -> {
            throw new IllegalStateException("provider secret sk-live-secret raw prompt " + prompt.getContents());
        };
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                LearningOsMetrics.noop(),
                chatModel
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);

        assertThatThrownBy(() -> gateway.generateStructuredWithRetry(
                modelRequest(),
                new AgentExecutionContext("agt_raw_error", "trace_raw_error"),
                recorder,
                failureTrace()
        ))
                .isInstanceOf(AiModelGateway.ModelCallFailedException.class)
                .hasMessage("MODEL_PROVIDER_ERROR")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("sk-live-secret", "raw prompt"));
    }

    @Test
    void normalizesUnsafeConfiguredProviderToOther() {
        ChatModel chatModel = prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("""
                {"summary":"safe provider normalization"}
                """))));
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("https://provider.example/v1?apiKey=sk-live-secret", "gpt-test", "embed-test"),
                LearningOsMetrics.noop(),
                chatModel
        );

        AiModelGateway.ModelResponse response = gateway.generateStructured(new AiModelGateway.ModelRequest(
                "critic-agent",
                "Review resource grounding.",
                Map.of()
        ));

        assertThat(response.provider()).isEqualTo("other");
        assertThat(response.provider()).doesNotContain("https://", "apiKey", "sk-live-secret");
    }

    @Test
    void retriesStructuredGenerationOnceAndReturnsSuccessWhenSecondAttemptSucceeds() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("none", "", ""),
                new LearningOsMetrics(meterRegistry)
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);
        AtomicInteger attempts = new AtomicInteger();

        AiModelGateway.ModelResponse response = gateway.generateStructuredWithRetry(
                modelRequest(),
                request -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("temporary provider timeout");
                    }
                    return gateway.generateStructured(request);
                },
                new AgentExecutionContext("agt_retry_success", "trace_retry_success"),
                recorder,
                failureTrace()
        );

        assertThat(attempts).hasValue(2);
        assertThat(response.status()).isEqualTo(AgentRuntimeConstants.MODEL_CALL_SUCCESS);
        assertThat(responseLatency(response)).isPositive();
        verifyNoInteractions(recorder);
        assertThat(meterRegistry.find("learningos.model.call.duration")
                .tag("agent_name", "resource-generator")
                .tag("provider", "none")
                .tag("model", AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO)
                .tag("status", AgentRuntimeConstants.MODEL_CALL_SUCCESS)
                .timer()
                .count()).isEqualTo(1);
    }

    @Test
    void recordsFailureWhenStructuredGenerationFailsTwice() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", ""),
                new LearningOsMetrics(meterRegistry)
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);
        AgentExecutionContext context = new AgentExecutionContext("agt_retry_failure", "trace_retry_failure");
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> gateway.generateStructuredWithRetry(
                modelRequest(),
                request -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("provider unavailable");
                },
                context,
                recorder,
                failureTrace()
        ))
                .isInstanceOf(AiModelGateway.ModelCallFailedException.class)
                .hasMessage("MODEL_PROVIDER_ERROR")
                .satisfies(error -> assertThat(error.getCause()).isNull());

        assertThat(attempts).hasValue(2);
        verify(recorder).recordFailure(
                eq(context),
                eq("step_resource"),
                eq("ResourceAgent"),
                eq("Model call failed while drafting resources."),
                anyLong(),
                eq(AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                eq("openai"),
                eq("MODEL_PROVIDER_ERROR")
        );
        assertThat(meterRegistry.find("learningos.model.call.duration")
                .tag("agent_name", "resource-generator")
                .tag("provider", "openai")
                .tag("status", AgentRuntimeConstants.MODEL_CALL_FAILED)
                .timer()
                .count()).isEqualTo(1);
        assertThat(meterRegistry.find("learningos.model.call.failures")
                .tag("agent_name", "resource-generator")
                .tag("error_code", "MODEL_PROVIDER_ERROR")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void rejectsStructuredResourceOutputWhenRequiredResourcesFieldIsMissing() {
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                LearningOsMetrics.noop()
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);
        AgentExecutionContext context = new AgentExecutionContext("agt_missing_resources", "trace_missing_resources");

        assertThatThrownBy(() -> gateway.generateStructuredWithRetry(
                modelRequest(),
                request -> new AiModelGateway.ModelResponse(
                        "openai",
                        "gpt-test",
                        AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                        "raw model output with sk-live-secret should not be persisted",
                        Map.of("summary", "missing resources"),
                        new AiModelGateway.TokenUsage(10, 5, 15, 0.01)
                ),
                context,
                recorder,
                failureTrace()
        ))
                .isInstanceOf(AiModelGateway.ModelCallFailedException.class)
                .hasMessage("STRUCTURED_OUTPUT_INVALID")
                .satisfies(error -> assertThat(error.getMessage())
                        .doesNotContain("sk-live-secret", "raw model output", "missing resources"));

        verify(recorder).recordFailure(
                eq(context),
                eq("step_resource"),
                eq("ResourceAgent"),
                eq("Model call failed while drafting resources."),
                anyLong(),
                eq(AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                eq("openai"),
                eq("STRUCTURED_OUTPUT_INVALID")
        );
    }

    @Test
    void rejectsStructuredResourceOutputWhenResourceItemMissesRequiredFields() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                new LearningOsMetrics(meterRegistry)
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);
        AgentExecutionContext context = new AgentExecutionContext("agt_missing_fields", "trace_missing_fields");

        assertThatThrownBy(() -> gateway.generateStructuredWithRetry(
                modelRequest(),
                request -> new AiModelGateway.ModelResponse(
                        "openai",
                        "gpt-test",
                        AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                        "resource item omitted required schema fields",
                        Map.of("resources", List.of(Map.of(
                                "title", "SQL JOIN remediation",
                                "type", "LECTURE"
                        ))),
                        new AiModelGateway.TokenUsage(10, 5, 15, 0.01)
                ),
                context,
                recorder,
                failureTrace()
        ))
                .isInstanceOf(AiModelGateway.ModelCallFailedException.class)
                .hasMessage("STRUCTURED_OUTPUT_INVALID")
                .satisfies(error -> assertThat(error.getMessage())
                        .doesNotContain("resource item omitted", "SQL JOIN remediation"));

        verify(recorder).recordFailure(
                eq(context),
                eq("step_resource"),
                eq("ResourceAgent"),
                eq("Model call failed while drafting resources."),
                anyLong(),
                eq(AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                eq("openai"),
                eq("STRUCTURED_OUTPUT_INVALID")
        );
        assertThat(meterRegistry.find("learningos.model.call.failures")
                .tag("agent_name", "resource-generator")
                .tag("error_code", "STRUCTURED_OUTPUT_INVALID")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void rejectsStructuredResourceOutputWhenSafetyStatusIsInvalid() {
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                LearningOsMetrics.noop()
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);
        AgentExecutionContext context = new AgentExecutionContext("agt_invalid_safety", "trace_invalid_safety");

        assertThatThrownBy(() -> gateway.generateStructuredWithRetry(
                modelRequest(),
                request -> new AiModelGateway.ModelResponse(
                        "openai",
                        "gpt-test",
                        AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                        "raw content should not be exposed",
                        Map.of("resources", List.of(Map.of(
                                "title", "SQL JOIN remediation",
                                "type", "LECTURE",
                                "modality", "MARKDOWN",
                                "markdownContent", "private generated draft",
                                "citationSummary", "COURSE_RAG",
                                "safetyStatus", "SAFE_BUT_UNSUPPORTED"
                        ))),
                        new AiModelGateway.TokenUsage(10, 5, 15, 0.01)
                ),
                context,
                recorder,
                failureTrace()
        ))
                .isInstanceOf(AiModelGateway.ModelCallFailedException.class)
                .hasMessage("STRUCTURED_OUTPUT_INVALID")
                .satisfies(error -> assertThat(error.getCause()).isNull())
                .satisfies(error -> assertThat(error.getMessage())
                        .doesNotContain("SAFE_BUT_UNSUPPORTED", "private generated draft", "raw content"));

        verify(recorder).recordFailure(
                eq(context),
                eq("step_resource"),
                eq("ResourceAgent"),
                eq("Model call failed while drafting resources."),
                anyLong(),
                eq(AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                eq("openai"),
                eq("STRUCTURED_OUTPUT_INVALID")
        );
    }

    @Test
    void sanitizesProviderFailureBeforeThrowingAndRecordingFailure() {
        AiModelGateway gateway = new AiModelGateway(
                new AiModelProperties("openai", "gpt-test", "embed-test"),
                LearningOsMetrics.noop()
        );
        AgentRunRecorder recorder = mock(AgentRunRecorder.class);
        AgentExecutionContext context = new AgentExecutionContext("agt_secret_failure", "trace_secret_failure");
        String rawProviderError = "401 raw provider error sk-live-secret raw prompt: private prompt "
                + "student answer: private answer RAG chunk: private course chunk";

        assertThatThrownBy(() -> gateway.generateStructuredWithRetry(
                modelRequest(),
                request -> {
                    throw new IllegalStateException(rawProviderError);
                },
                context,
                recorder,
                failureTrace()
        ))
                .isInstanceOf(AiModelGateway.ModelCallFailedException.class)
                .hasMessage("MODEL_PROVIDER_ERROR")
                .satisfies(error -> assertThat(error.getCause()).isNull())
                .satisfies(error -> assertThat(error.getMessage())
                        .doesNotContain("sk-live-secret", "raw prompt", "student answer", "RAG chunk", "401 raw"));

        verify(recorder).recordFailure(
                eq(context),
                eq("step_resource"),
                eq("ResourceAgent"),
                eq("Model call failed while drafting resources."),
                anyLong(),
                eq(AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                eq("openai"),
                eq("MODEL_PROVIDER_ERROR")
        );
    }

    private AiModelGateway.ModelRequest modelRequest() {
        return new AiModelGateway.ModelRequest(
                "resource-generator",
                "Generate a short SQL JOIN remediation resource.",
                Map.of("learnerId", "alice", "pathNodeId", "node_sql_join"),
                AgentRuntimeConstants.PROMPT_RESOURCE_V1
        );
    }

    private AiModelGateway.FailureTrace failureTrace() {
        return new AiModelGateway.FailureTrace(
                "step_resource",
                "ResourceAgent",
                "Model call failed while drafting resources.",
                AgentRuntimeConstants.PROMPT_RESOURCE_V1
        );
    }

    private long responseLatency(AiModelGateway.ModelResponse response) {
        try {
            Method method = response.getClass().getMethod("latencyMs");
            Object value = method.invoke(response);
            return ((Number) value).longValue();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("ModelResponse should expose gateway latencyMs", exception);
        }
    }

    private record FixedUsage(Integer promptTokens, Integer completionTokens) implements Usage {

        @Override
        public Integer getPromptTokens() {
            return promptTokens;
        }

        @Override
        public Integer getCompletionTokens() {
            return completionTokens;
        }

        @Override
        public Object getNativeUsage() {
            return Map.of();
        }
    }
}
