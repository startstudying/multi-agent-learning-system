package com.learningos.agent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.common.observability.LearningOsMetrics;
import com.learningos.config.AiModelProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class AiModelGateway {

    private static final int MAX_MODEL_ATTEMPTS = 2;
    private static final String SAFE_MODEL_PROVIDER_ERROR = "MODEL_PROVIDER_ERROR";
    private static final Set<String> RESOURCE_SAFETY_STATUSES = Set.of("APPROVED", "NEEDS_REVIEW", "BLOCKED");
    private static final Set<String> SAFE_MODEL_PROVIDERS = Set.of(
            "none", "openai", "dashscope", "deepseek", "mimo", "anthropic", "gemini", "mock", "custom"
    );

    private final AiModelProperties properties;
    private final LearningOsMetrics metrics;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final ModelProviderService modelProviderService;
    private final ChatModelFactory chatModelFactory;
    private final TokenBudgetGateService tokenBudgetGateService;

    public AiModelGateway(AiModelProperties properties) {
        this(properties, LearningOsMetrics.noop(), null, null, null, null, null);
    }

    @Autowired
    public AiModelGateway(
            AiModelProperties properties,
            ObjectProvider<LearningOsMetrics> metricsProvider,
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<ModelProviderService> modelProviderServiceProvider,
            ObjectProvider<ChatModelFactory> chatModelFactoryProvider,
            ObjectProvider<TokenBudgetGateService> tokenBudgetGateServiceProvider
    ) {
        this(
                properties,
                metricsProvider.getIfAvailable(LearningOsMetrics::noop),
                chatModelProvider.getIfAvailable(),
                objectMapperProvider.getIfAvailable(ObjectMapper::new),
                modelProviderServiceProvider.getIfAvailable(),
                chatModelFactoryProvider.getIfAvailable(),
                tokenBudgetGateServiceProvider.getIfAvailable()
        );
    }

    public AiModelGateway(AiModelProperties properties, LearningOsMetrics metrics) {
        this(properties, metrics, null, null, null, null, null);
    }

    AiModelGateway(AiModelProperties properties, LearningOsMetrics metrics, ChatModel chatModel) {
        this(properties, metrics, chatModel, new ObjectMapper(), null, null, null);
    }

    AiModelGateway(
            AiModelProperties properties,
            LearningOsMetrics metrics,
            ChatModel chatModel,
            ObjectMapper objectMapper,
            ModelProviderService modelProviderService,
            ChatModelFactory chatModelFactory
    ) {
        this(properties, metrics, chatModel, objectMapper, modelProviderService, chatModelFactory, null);
    }

    AiModelGateway(
            AiModelProperties properties,
            LearningOsMetrics metrics,
            ChatModel chatModel,
            ObjectMapper objectMapper,
            ModelProviderService modelProviderService,
            ChatModelFactory chatModelFactory,
            TokenBudgetGateService tokenBudgetGateService
    ) {
        this.properties = properties;
        this.metrics = metrics;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.modelProviderService = modelProviderService;
        this.chatModelFactory = chatModelFactory;
        this.tokenBudgetGateService = tokenBudgetGateService;
    }

    public ModelResponse generateStructured(ModelRequest request) {
        long startedAt = System.nanoTime();
        if (shouldUseDeterministicFallback()) {
            return deterministicStructuredResponse(request, normalizeProvider(), startedAt, "token_budget_gate");
        }
        if (modelProviderService != null && chatModelFactory != null) {
            var registryProvider = modelProviderService.resolveDefaultChatProvider();
            if (registryProvider.isPresent()) {
                ModelProviderService.ResolvedChatProvider resolved = registryProvider.get();
                ChatModel dynamicChatModel = chatModelFactory.createChatModel(
                        resolved.baseUrl(),
                        resolved.apiKey(),
                        resolved.chatModel()
                );
                return generateFromChatModel(
                        request,
                        dynamicChatModel,
                        normalizeProviderCode(resolved.providerCode()),
                        resolved.chatModel(),
                        startedAt
                );
            }
        }
        String provider = normalizeProvider();
        boolean chatConfigured = properties.configured() && hasText(properties.chatModel());
        String model = chatConfigured
                ? properties.chatModel()
                : AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO;
        if (chatConfigured) {
            if (chatModel == null) {
                throw new ModelProviderUnavailableException();
            }
            return generateFromChatModel(request, chatModel, provider, model, startedAt);
        }

        return deterministicStructuredResponse(request, provider, startedAt, null);
    }

    private boolean shouldUseDeterministicFallback() {
        if (tokenBudgetGateService == null) {
            return false;
        }
        TokenBudgetGateService.TokenBudgetDecision decision = tokenBudgetGateService.evaluate();
        return decision.overBudget()
                && TokenBudgetGateService.FALLBACK_DETERMINISTIC_ONLY.equals(decision.fallbackStrategy());
    }

    private ModelResponse deterministicStructuredResponse(
            ModelRequest request,
            String provider,
            long startedAt,
            String budgetGateReason
    ) {
        String model = AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO;
        Map<String, Object> structuredOutput = new LinkedHashMap<>();
        structuredOutput.put("mode", "deterministic");
        structuredOutput.put("externalCall", false);
        structuredOutput.put("agentName", request.agentName());
        structuredOutput.put("context", request.context());
        if (budgetGateReason != null) {
            structuredOutput.put("budgetGateReason", budgetGateReason);
        }
        structuredOutput.put("summary", "Structured model output placeholder for " + request.agentName() + ".");
        if (AgentRuntimeConstants.PROMPT_RESOURCE_V1.equals(request.promptVersion())) {
            structuredOutput.put("resources", List.of(Map.of(
                    "title", "Gateway validated resource draft",
                    "type", "LECTURE",
                    "modality", "MARKDOWN",
                    "markdownContent", "### Gateway validated resource draft\nThis deterministic draft awaits review.",
                    "citationSummary", "COURSE_RAG: gateway placeholder requires persisted citations before release.",
                    "safetyStatus", AgentRuntimeConstants.SAFETY_NEEDS_REVIEW
            )));
        }

        String content = "Generated structured output for " + request.agentName() + " using " + model + ".";
        TokenUsage tokenUsage = estimateUsage(request, content);
        return new ModelResponse(
                provider,
                model,
                AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                content,
                structuredOutput,
                tokenUsage,
                Math.max(1L, latencyMs(startedAt))
        );
    }

    private ModelResponse generateFromChatModel(
            ModelRequest request,
            ChatModel activeChatModel,
            String provider,
            String configuredModel,
            long startedAt
    ) {
        try {
            ChatResponse chatResponse = activeChatModel.call(new Prompt(providerPrompt(request)));
            String content = firstText(chatResponse);
            Map<String, Object> structuredOutput = parseStructuredJson(content);
            String responseModel = safeModel(chatResponse == null || chatResponse.getMetadata() == null
                    ? null
                    : chatResponse.getMetadata().getModel(), configuredModel);
            TokenUsage tokenUsage = tokenUsage(chatResponse, request, content);
            return new ModelResponse(
                    provider,
                    responseModel,
                    AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                    content,
                    structuredOutput,
                    tokenUsage,
                    Math.max(1L, latencyMs(startedAt))
            );
        } catch (StructuredOutputValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ModelProviderUnavailableException();
        }
    }

    private String providerPrompt(ModelRequest request) {
        return """
                You are a backend model adapter for Learning OS.
                Return strict JSON only. Do not include markdown fences or commentary.
                If the prompt version is agent-resource-v1, return:
                {"resources":[{"title":"","type":"","modality":"","markdownContent":"","citationSummary":"","safetyStatus":"NEEDS_REVIEW"}]}
                Agent: %s
                Prompt version: %s
                User prompt:
                %s
                Safe context:
                %s
                """.formatted(
                request.agentName(),
                request.promptVersion(),
                request.prompt(),
                request.context()
        );
    }

    private String firstText(ChatResponse response) {
        if (response == null) {
            throw new StructuredOutputValidationException();
        }
        Generation result = response.getResult();
        if (result == null || result.getOutput() == null || !hasText(result.getOutput().getText())) {
            throw new StructuredOutputValidationException();
        }
        return result.getOutput().getText();
    }

    private Map<String, Object> parseStructuredJson(String content) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(content, new TypeReference<>() {
            });
            return parsed == null ? Map.of() : parsed;
        } catch (JsonProcessingException ex) {
            throw new StructuredOutputValidationException();
        }
    }

    private TokenUsage tokenUsage(ChatResponse chatResponse, ModelRequest request, String content) {
        if (chatResponse != null && chatResponse.getMetadata() != null) {
            Usage usage = chatResponse.getMetadata().getUsage();
            if (usage != null && positiveUsage(usage)) {
                int promptTokens = nonNegative(usage.getPromptTokens());
                int completionTokens = nonNegative(usage.getCompletionTokens());
                int totalTokens = Math.max(nonNegative(usage.getTotalTokens()), promptTokens + completionTokens);
                return new TokenUsage(promptTokens, completionTokens, totalTokens, 0.0);
            }
        }
        return estimateUsage(request, content);
    }

    private boolean positiveUsage(Usage usage) {
        return nonNegative(usage.getPromptTokens()) > 0
                || nonNegative(usage.getCompletionTokens()) > 0
                || nonNegative(usage.getTotalTokens()) > 0;
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String safeModel(String candidate, String fallback) {
        if (!hasText(candidate)) {
            return fallback;
        }
        String trimmed = candidate.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("sk-") || lower.contains("apikey") || lower.contains("api_key")
                || lower.contains("secret") || lower.contains("https://") || lower.contains("http://")) {
            return fallback;
        }
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    public ModelResponse generateStructuredWithRetry(
            ModelRequest request,
            AgentExecutionContext executionContext,
            AgentRunRecorder recorder,
            FailureTrace failureTrace
    ) {
        return generateStructuredWithRetry(request, this::generateStructured, executionContext, recorder, failureTrace);
    }

    ModelResponse generateStructuredWithRetry(
            ModelRequest request,
            ModelOperation operation,
            AgentExecutionContext executionContext,
            AgentRunRecorder recorder,
            FailureTrace failureTrace
    ) {
        long startedAt = System.nanoTime();
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_MODEL_ATTEMPTS; attempt++) {
            try {
                ModelResponse response = operation.generate(request);
                validateStructuredOutput(response, schemaPromptVersion(request, failureTrace));
                metrics.recordModelCall(
                        request.agentName(),
                        response.provider(),
                        response.model(),
                        response.status(),
                        "OK",
                        response.latencyMs()
                );
                return response;
            } catch (RuntimeException ex) {
                lastFailure = ex;
            }
        }

        long latencyMs = Math.max(1L, latencyMs(startedAt));
        String errorMessage = safeErrorCode(lastFailure);
        String provider = normalizeProvider();
        metrics.recordModelCall(
                request.agentName(),
                provider,
                configuredModel(),
                AgentRuntimeConstants.MODEL_CALL_FAILED,
                errorMessage,
                latencyMs
        );
        recorder.recordFailure(
                executionContext,
                failureTrace.stepId(),
                failureTrace.agentName(),
                failureTrace.summary(),
                latencyMs,
                failureTrace.promptVersion(),
                provider,
                errorMessage
        );
        throw new ModelCallFailedException(errorMessage);
    }

    private String schemaPromptVersion(ModelRequest request, FailureTrace failureTrace) {
        if (hasText(request.promptVersion())) {
            return request.promptVersion();
        }
        return failureTrace == null ? null : failureTrace.promptVersion();
    }

    private void validateStructuredOutput(ModelResponse response, String promptVersion) {
        if (!AgentRuntimeConstants.PROMPT_RESOURCE_V1.equals(promptVersion)) {
            return;
        }
        Object resourcesValue = response.structuredOutput().get("resources");
        if (!(resourcesValue instanceof List<?> resources) || resources.isEmpty()) {
            throw new StructuredOutputValidationException();
        }
        for (Object item : resources) {
            if (!(item instanceof Map<?, ?> resource)) {
                throw new StructuredOutputValidationException();
            }
            requireText(resource, "title");
            requireText(resource, "type");
            requireText(resource, "modality");
            requireText(resource, "markdownContent");
            requireText(resource, "citationSummary");
            String safetyStatus = requireText(resource, "safetyStatus");
            if (!RESOURCE_SAFETY_STATUSES.contains(safetyStatus)) {
                throw new StructuredOutputValidationException();
            }
        }
    }

    private String requireText(Map<?, ?> resource, String field) {
        Object value = resource.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new StructuredOutputValidationException();
        }
        return text;
    }

    private TokenUsage estimateUsage(ModelRequest request, String content) {
        int promptTokens = estimateTokens(request.prompt()) + estimateTokens(String.valueOf(request.context()));
        int completionTokens = estimateTokens(content) + 24;
        return new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens, 0.0);
    }

    private String safeErrorCode(RuntimeException failure) {
        if (failure instanceof StructuredOutputValidationException) {
            return AgentRuntimeConstants.MODEL_STRUCTURED_OUTPUT_INVALID;
        }
        return SAFE_MODEL_PROVIDER_ERROR;
    }

    private int estimateTokens(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(value.length() / 4.0));
    }

    private String normalizeProvider() {
        return normalizeProviderCode(properties.provider());
    }

    private String normalizeProviderCode(String provider) {
        if (!hasText(provider)) {
            return "none";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return SAFE_MODEL_PROVIDERS.contains(normalized) ? normalized : "other";
    }

    private String configuredModel() {
        return properties.configured() && hasText(properties.chatModel())
                ? properties.chatModel()
                : AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO;
    }

    private long latencyMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ModelRequest(
            String agentName,
            String prompt,
            Map<String, ?> context,
            String promptVersion
    ) {
        public ModelRequest(String agentName, String prompt, Map<String, ?> context) {
            this(agentName, prompt, context, null);
        }

        public ModelRequest {
            context = context == null ? Map.of() : Map.copyOf(context);
        }
    }

    public record ModelResponse(
            String provider,
            String model,
            String status,
            String content,
            Map<String, Object> structuredOutput,
            TokenUsage tokenUsage,
            long latencyMs
    ) {
        public ModelResponse(
                String provider,
                String model,
                String status,
                String content,
                Map<String, Object> structuredOutput,
                TokenUsage tokenUsage
        ) {
            this(provider, model, status, content, structuredOutput, tokenUsage, 1L);
        }

        public ModelResponse {
            provider = safeProvider(provider);
            structuredOutput = structuredOutput == null ? Map.of() : Map.copyOf(structuredOutput);
            latencyMs = Math.max(1L, latencyMs);
        }

        private static String safeProvider(String provider) {
            if (provider == null || provider.isBlank()) {
                return "none";
            }
            String normalized = provider.trim().toLowerCase(Locale.ROOT);
            return SAFE_MODEL_PROVIDERS.contains(normalized) || "other".equals(normalized) ? normalized : "other";
        }
    }

    public record TokenUsage(
            int promptTokens,
            int completionTokens,
            int totalTokens,
            double estimatedCost
    ) {
    }

    @FunctionalInterface
    interface ModelOperation {
        ModelResponse generate(ModelRequest request);
    }

    public record FailureTrace(
            String stepId,
            String agentName,
            String summary,
            String promptVersion
    ) {
    }

    public static class ModelCallFailedException extends RuntimeException {

        public ModelCallFailedException(String message) {
            super(message);
        }
    }

    private static class StructuredOutputValidationException extends RuntimeException {

        private StructuredOutputValidationException() {
            super(AgentRuntimeConstants.MODEL_STRUCTURED_OUTPUT_INVALID);
        }
    }

    private static class ModelProviderUnavailableException extends RuntimeException {

        private ModelProviderUnavailableException() {
            super(SAFE_MODEL_PROVIDER_ERROR);
        }
    }
}
