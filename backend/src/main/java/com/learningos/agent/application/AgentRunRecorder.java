package com.learningos.agent.application;

import com.learningos.agent.domain.AgentTask;
import com.learningos.agent.domain.AgentToolCall;
import com.learningos.agent.domain.AgentTrace;
import com.learningos.agent.domain.ModelCallLog;
import com.learningos.agent.domain.TokenUsageLog;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentToolCallRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.agent.repository.ModelCallLogRepository;
import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.common.observability.LearningOsMetrics;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AgentRunRecorder {

    private static final double DEFAULT_MODEL_TEMPERATURE = 0.0;
    private static final String SAFE_MODEL_PROVIDER_ERROR = "MODEL_PROVIDER_ERROR";
    private static final String SAFE_TOOL_ERROR = "TOOL_CALL_FAILED";
    private static final String TOOL_RETENTION_CLASS = "SANITIZED_SUMMARY";
    private static final Set<String> SAFE_MODEL_PROVIDERS = Set.of(
            "none", "openai", "dashscope", "deepseek", "mimo", "anthropic", "gemini", "mock", "other", "custom"
    );

    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final AgentToolCallRepository agentToolCallRepository;
    private final ModelCallLogRepository modelCallLogRepository;
    private final TokenUsageLogRepository tokenUsageLogRepository;
    private final LearningOsMetrics metrics;

    public AgentRunRecorder(
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            AgentToolCallRepository agentToolCallRepository,
            ModelCallLogRepository modelCallLogRepository,
            TokenUsageLogRepository tokenUsageLogRepository,
            ObjectProvider<LearningOsMetrics> metricsProvider
    ) {
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.agentToolCallRepository = agentToolCallRepository;
        this.modelCallLogRepository = modelCallLogRepository;
        this.tokenUsageLogRepository = tokenUsageLogRepository;
        this.metrics = metricsProvider.getIfAvailable(LearningOsMetrics::noop);
    }

    @Transactional
    public AgentExecutionContext startRun(RunStart runStart) {
        validateInitialTaskStatus(runStart.status());
        String agentTaskId = id("agt");

        AgentTask agentTask = new AgentTask();
        agentTask.setId(agentTaskId);
        agentTask.setOwnerUserId(runStart.ownerUserId());
        agentTask.setTaskType(runStart.taskType());
        agentTask.setStatus(runStart.status());
        agentTask.setInputJson(runStart.inputJson());
        agentTask.setOutputJson(runStart.outputJson());
        agentTask.setTraceId(runStart.traceId());
        agentTask.setLatencyMs(runStart.latencyMs());
        agentTaskRepository.save(agentTask);

        return new AgentExecutionContext(agentTaskId, runStart.traceId());
    }

    @Transactional
    public List<AgentTrace> recordTraceSteps(AgentExecutionContext context, List<TraceStep> steps) {
        List<AgentTrace> traces = new ArrayList<>();
        int sequenceOffset = (int) agentTraceRepository.countByAgentTaskId(context.agentTaskId());
        for (int index = 0; index < steps.size(); index++) {
            TraceStep step = steps.get(index);
            validateTraceStatus(step.status());
            AgentTrace trace = new AgentTrace();
            trace.setId(id("atr"));
            trace.setAgentTaskId(context.agentTaskId());
            trace.setStepId(step.stepId());
            trace.setAgentName(step.agentName());
            trace.setStatus(step.status());
            trace.setSummary(step.summary());
            trace.setLatencyMs(step.latencyMs());
            trace.setModel(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
            trace.setPromptVersion(step.promptVersion());
            trace.setTraceId(context.traceId());
            trace.setSequenceNo(sequenceOffset + index + 1);
            traces.add(trace);
        }
        return agentTraceRepository.saveAll(traces);
    }

    @Transactional
    public void recordSuccessfulModelEvidence(
            AgentExecutionContext context,
            AgentTrace firstTrace,
            TokenUsageEstimate tokenUsage
    ) {
        recordSuccessfulModelEvidence(
                context,
                firstTrace.getPromptVersion(),
                firstTrace.getModel(),
                "none",
                firstTrace.getLatencyMs(),
                tokenUsage.promptTokens(),
                tokenUsage.completionTokens(),
                tokenUsage.totalTokens(),
                tokenUsage.estimatedCost()
        );
    }

    @Transactional
    public void recordSuccessfulModelEvidence(
            AgentExecutionContext context,
            AgentTrace modelTrace,
            AiModelGateway.ModelResponse modelResponse
    ) {
        AiModelGateway.TokenUsage tokenUsage = modelResponse.tokenUsage();
        recordSuccessfulModelEvidence(
                context,
                modelTrace.getPromptVersion(),
                modelResponse.model(),
                modelResponse.provider(),
                modelResponse.latencyMs(),
                tokenUsage.promptTokens(),
                tokenUsage.completionTokens(),
                tokenUsage.totalTokens(),
                tokenUsage.estimatedCost()
        );
    }

    private void recordSuccessfulModelEvidence(
            AgentExecutionContext context,
            String promptVersion,
            String model,
            String provider,
            Long latencyMs,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Double estimatedCost
    ) {
        ModelCallLog modelCallLog = new ModelCallLog();
        modelCallLog.setId(id("mcl"));
        modelCallLog.setTraceId(context.traceId());
        modelCallLog.setAgentTaskId(context.agentTaskId());
        modelCallLog.setModel(model);
        modelCallLog.setProvider(safeProvider(provider));
        applyPromptMetadata(modelCallLog, promptVersion);
        modelCallLog.setStatus(AgentRuntimeConstants.MODEL_CALL_SUCCESS);
        modelCallLog.setLatencyMs(latencyMs);
        modelCallLog.setEstimatedCost(estimatedCost);
        modelCallLogRepository.save(modelCallLog);

        TokenUsageLog tokenUsageLog = new TokenUsageLog();
        tokenUsageLog.setId(id("tul"));
        tokenUsageLog.setTraceId(context.traceId());
        tokenUsageLog.setAgentTaskId(context.agentTaskId());
        tokenUsageLog.setPromptTokens(promptTokens);
        tokenUsageLog.setCompletionTokens(completionTokens);
        tokenUsageLog.setTotalTokens(totalTokens);
        tokenUsageLog.setEstimatedCost(estimatedCost);
        tokenUsageLogRepository.save(tokenUsageLog);
        metrics.recordTokenUsage(
                modelCallLog.getModel(),
                modelCallLog.getPromptCode(),
                promptTokens,
                completionTokens,
                totalTokens,
                estimatedCost
        );
    }

    @Transactional
    public AgentTrace recordFailure(
            AgentExecutionContext context,
            String stepId,
            String agentName,
            String summary,
            Long latencyMs,
            String promptVersion,
            String errorMessage
    ) {
        return recordFailure(context, stepId, agentName, summary, latencyMs, promptVersion, "none", errorMessage);
    }

    @Transactional
    public AgentTrace recordFailure(
            AgentExecutionContext context,
            String stepId,
            String agentName,
            String summary,
            Long latencyMs,
            String promptVersion,
            String provider,
            String errorMessage
    ) {
        String sanitizedErrorMessage = sanitizeModelError(errorMessage);
        String failureSummary = summary + " Error: " + sanitizedErrorMessage;
        AgentTask agentTask = agentTaskRepository.findById(context.agentTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Agent task not found: " + context.agentTaskId()));
        validateTransition(agentTask.getStatus(), AgentRuntimeConstants.STATUS_FAILED);
        agentTask.setStatus(AgentRuntimeConstants.STATUS_FAILED);
        agentTask.setOutputJson("""
                {"status":"FAILED","summary":"%s","errorMessage":"%s","recoverable":true}
                """.formatted(escapeJson(summary), escapeJson(sanitizedErrorMessage)).trim());
        agentTask.setLatencyMs(latencyMs);
        agentTaskRepository.save(agentTask);

        int nextSequenceNo = (int) agentTraceRepository.countByAgentTaskId(context.agentTaskId()) + 1;
        AgentTrace trace = new AgentTrace();
        trace.setId(id("atr"));
        trace.setAgentTaskId(context.agentTaskId());
        trace.setStepId(stepId);
        trace.setAgentName(agentName);
        trace.setStatus(AgentRuntimeConstants.STATUS_FAILED);
        trace.setSummary(failureSummary);
        trace.setLatencyMs(latencyMs);
        trace.setModel(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
        trace.setPromptVersion(promptVersion);
        trace.setTraceId(context.traceId());
        trace.setSequenceNo(nextSequenceNo);
        AgentTrace savedTrace = agentTraceRepository.save(trace);

        ModelCallLog modelCallLog = new ModelCallLog();
        modelCallLog.setId(id("mcl"));
        modelCallLog.setTraceId(context.traceId());
        modelCallLog.setAgentTaskId(context.agentTaskId());
        modelCallLog.setModel(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
        modelCallLog.setProvider(safeProvider(provider));
        applyPromptMetadata(modelCallLog, promptVersion);
        modelCallLog.setStatus(AgentRuntimeConstants.MODEL_CALL_FAILED);
        modelCallLog.setLatencyMs(latencyMs);
        modelCallLog.setErrorMessage(sanitizedErrorMessage);
        modelCallLog.setEstimatedCost(0.0);
        modelCallLogRepository.save(modelCallLog);

        return savedTrace;
    }

    @Transactional
    public AgentTrace recordRuntimeFailure(
            AgentExecutionContext context,
            String stepId,
            String agentName,
            String summary,
            Long latencyMs,
            String promptVersion,
            String errorMessage
    ) {
        String failureSummary = summary + " Error: " + errorMessage;
        AgentTask agentTask = agentTaskRepository.findById(context.agentTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Agent task not found: " + context.agentTaskId()));
        validateTransition(agentTask.getStatus(), AgentRuntimeConstants.STATUS_FAILED);
        agentTask.setStatus(AgentRuntimeConstants.STATUS_FAILED);
        agentTask.setOutputJson("""
                {"status":"FAILED","summary":"%s","errorMessage":"%s","recoverable":true}
                """.formatted(escapeJson(summary), escapeJson(errorMessage)).trim());
        agentTask.setLatencyMs(latencyMs);
        agentTaskRepository.save(agentTask);

        int nextSequenceNo = (int) agentTraceRepository.countByAgentTaskId(context.agentTaskId()) + 1;
        AgentTrace trace = new AgentTrace();
        trace.setId(id("atr"));
        trace.setAgentTaskId(context.agentTaskId());
        trace.setStepId(stepId);
        trace.setAgentName(agentName);
        trace.setStatus(AgentRuntimeConstants.STATUS_FAILED);
        trace.setSummary(failureSummary);
        trace.setLatencyMs(latencyMs);
        trace.setModel(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
        trace.setPromptVersion(promptVersion);
        trace.setTraceId(context.traceId());
        trace.setSequenceNo(nextSequenceNo);
        return agentTraceRepository.save(trace);
    }

    @Transactional
    public void recordToolCall(
            AgentExecutionContext context,
            String toolName,
            String inputJson,
            String outputJson,
            String status,
            String errorMessage,
            Long latencyMs
    ) {
        validateTraceStatus(status);
        AgentToolCall toolCall = new AgentToolCall();
        toolCall.setId(id("atc"));
        toolCall.setAgentTaskId(context.agentTaskId());
        toolCall.setTraceId(context.traceId());
        toolCall.setToolName(requiredToolName(toolName));
        toolCall.setInputJson(sanitizeToolText(inputJson));
        toolCall.setOutputJson(sanitizeToolText(outputJson));
        toolCall.setInputSummary(summarizeToolText(inputJson));
        toolCall.setOutputSummary(summarizeToolText(outputJson));
        toolCall.setStatus(status);
        toolCall.setErrorMessage(AgentRuntimeConstants.STATUS_FAILED.equals(status) ? SAFE_TOOL_ERROR : sanitizeOptional(errorMessage));
        toolCall.setLatencyMs(latencyMs == null ? 0L : latencyMs);
        toolCall.setRetentionClass(TOOL_RETENTION_CLASS);
        agentToolCallRepository.save(toolCall);
    }

    @Transactional
    public void transitionTask(AgentExecutionContext context, String nextStatus, String summary, Long latencyMs) {
        transitionTask(context.agentTaskId(), nextStatus, summary, latencyMs);
    }

    @Transactional
    public void transitionTask(String agentTaskId, String nextStatus, String summary, Long latencyMs) {
        validateTaskStatus(nextStatus);
        AgentTask agentTask = agentTaskRepository.findById(agentTaskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Agent task not found"));
        validateTransition(agentTask.getStatus(), nextStatus);
        agentTask.setStatus(nextStatus);
        agentTask.setOutputJson("""
                {"status":"%s","summary":"%s"}
                """.formatted(escapeJson(nextStatus), escapeJson(summary)).trim());
        agentTask.setLatencyMs(latencyMs == null ? agentTask.getLatencyMs() : latencyMs);
        agentTaskRepository.save(agentTask);
    }

    @Transactional
    public AgentTrace cancelTask(String ownerUserId, String agentTaskId, String reason) {
        AgentTask agentTask = agentTaskRepository.findById(agentTaskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Agent task not found"));
        if (!ownerUserId.equals(agentTask.getOwnerUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Agent task access denied");
        }
        if (AgentRuntimeConstants.TERMINAL_STATUSES.contains(agentTask.getStatus())) {
            throw new ApiException(ErrorCode.CONFLICT, "Terminal agent task cannot be cancelled");
        }
        if (!AgentRuntimeConstants.CANCELLABLE_TASK_STATUSES.contains(agentTask.getStatus())) {
            throw new ApiException(ErrorCode.CONFLICT, "Agent task cannot be cancelled from status " + agentTask.getStatus());
        }

        String normalizedReason = reason == null || reason.isBlank()
                ? "Agent task was cancelled."
                : reason.trim();
        agentTask.setStatus(AgentRuntimeConstants.STATUS_CANCELLED);
        agentTask.setOutputJson("""
                {"status":"CANCELLED","summary":"%s","reason":"%s","recoverable":false}
                """.formatted(escapeJson(normalizedReason), escapeJson(normalizedReason)).trim());
        agentTaskRepository.save(agentTask);

        int nextSequenceNo = (int) agentTraceRepository.countByAgentTaskId(agentTaskId) + 1;
        AgentTrace trace = new AgentTrace();
        trace.setId(id("atr"));
        trace.setAgentTaskId(agentTaskId);
        trace.setStepId("task_cancelled");
        trace.setAgentName("OrchestratorAgent");
        trace.setStatus(AgentRuntimeConstants.STATUS_CANCELLED);
        trace.setSummary(normalizedReason);
        trace.setLatencyMs(0L);
        trace.setModel(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
        trace.setPromptVersion(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
        trace.setTraceId(agentTask.getTraceId());
        trace.setSequenceNo(nextSequenceNo);
        return agentTraceRepository.save(trace);
    }

    private void validateInitialTaskStatus(String status) {
        validateTaskStatus(status);
        if (!AgentRuntimeConstants.STATUS_PENDING.equals(status) && !AgentRuntimeConstants.STATUS_RUNNING.equals(status)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Agent task can only start from PENDING or RUNNING");
        }
    }

    private void validateTaskStatus(String status) {
        if (!AgentRuntimeConstants.TASK_STATUSES.contains(status)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid agent task status: " + status);
        }
    }

    private void validateTraceStatus(String status) {
        if (!AgentRuntimeConstants.TRACE_STATUSES.contains(status)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid agent trace status: " + status);
        }
    }

    private void validateTransition(String currentStatus, String nextStatus) {
        validateTaskStatus(currentStatus);
        validateTaskStatus(nextStatus);
        boolean allowed = switch (currentStatus) {
            case AgentRuntimeConstants.STATUS_PENDING -> AgentRuntimeConstants.STATUS_RUNNING.equals(nextStatus)
                    || AgentRuntimeConstants.STATUS_CANCELLED.equals(nextStatus);
            case AgentRuntimeConstants.STATUS_RUNNING -> AgentRuntimeConstants.STATUS_WAITING_REVIEW.equals(nextStatus)
                    || AgentRuntimeConstants.STATUS_DONE.equals(nextStatus)
                    || AgentRuntimeConstants.STATUS_FAILED.equals(nextStatus)
                    || AgentRuntimeConstants.STATUS_CANCELLED.equals(nextStatus);
            case AgentRuntimeConstants.STATUS_WAITING_REVIEW -> AgentRuntimeConstants.STATUS_DONE.equals(nextStatus)
                    || AgentRuntimeConstants.STATUS_FAILED.equals(nextStatus)
                    || AgentRuntimeConstants.STATUS_CANCELLED.equals(nextStatus);
            default -> false;
        };
        if (!allowed) {
            throw new ApiException(ErrorCode.CONFLICT,
                    "Invalid agent task transition: " + currentStatus + " -> " + nextStatus);
        }
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void applyPromptMetadata(ModelCallLog modelCallLog, String promptVersion) {
        PromptMetadata metadata = promptMetadata(promptVersion);
        modelCallLog.setPromptCode(metadata.promptCode());
        modelCallLog.setPromptVersion(promptVersion);
        modelCallLog.setTemperature(DEFAULT_MODEL_TEMPERATURE);
        modelCallLog.setStructuredOutputSchema(metadata.structuredOutputSchema());
    }

    private PromptMetadata promptMetadata(String promptVersion) {
        if (AgentRuntimeConstants.PROMPT_RESOURCE_V1.equals(promptVersion)) {
            return new PromptMetadata(
                    "resource-generation",
                    "ResourceGenerationOutputSchema{resources:[title,type,modality,markdownContent,citationSummary,safetyStatus]}"
            );
        }
        if (AgentRuntimeConstants.PROMPT_CRITIC_V1.equals(promptVersion)) {
            return new PromptMetadata(
                    "critic-review",
                    "CriticReviewOutputSchema{status,summary,citationCheck,safetyCheck,revisionSuggestion}"
            );
        }
        if (AgentRuntimeConstants.PROMPT_TUTOR_V1.equals(promptVersion)) {
            return new PromptMetadata(
                    "tutor-response",
                    "TutorResponseSchema{answer,steps,citations,nextAction}"
            );
        }
        if (AgentRuntimeConstants.PROMPT_SAFETY_V1.equals(promptVersion)) {
            return new PromptMetadata(
                    "safety-review",
                    "SafetyReviewSchema{status,reason,flags}"
            );
        }
        return new PromptMetadata("unknown", "GenericStructuredOutputSchema{}");
    }

    private String sanitizeModelError(String errorMessage) {
        if (AgentRuntimeConstants.MODEL_STRUCTURED_OUTPUT_INVALID.equals(errorMessage)) {
            return AgentRuntimeConstants.MODEL_STRUCTURED_OUTPUT_INVALID;
        }
        if (errorMessage == null || errorMessage.isBlank()) {
            return SAFE_MODEL_PROVIDER_ERROR;
        }
        return SAFE_MODEL_PROVIDER_ERROR;
    }

    private String safeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "none";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return SAFE_MODEL_PROVIDERS.contains(normalized) ? normalized : "other";
    }

    private String requiredToolName(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tool name is required");
        }
        return toolName.trim();
    }

    private String summarizeToolText(String value) {
        String sanitized = sanitizeToolText(value);
        if (sanitized.length() <= 500) {
            return sanitized;
        }
        return sanitized.substring(0, 500);
    }

    private String sanitizeToolText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = value
                .replaceAll("(?i)sk-[a-z0-9_-]+", "[REDACTED]")
                .replaceAll("(?i)(token|secret|password|apiKey)\"?\\s*[:=]\\s*\"?[^\"]+", "$1\":\"[REDACTED]")
                .replaceAll("(?i)(privateDocument|rawDocument|fullText)\"?\\s*:\\s*\"[^\"]*\"", "$1\":\"[REDACTED_DOCUMENT]\"");
        if (sanitized.toLowerCase().contains("private course document")
                || sanitized.toLowerCase().contains("private output")) {
            sanitized = sanitized.replaceAll("(?i)full private course document", "[REDACTED_DOCUMENT]")
                    .replaceAll("(?i)full generated private output", "[REDACTED_DOCUMENT]")
                    .replaceAll("(?i)raw private output", "[REDACTED_DOCUMENT]");
        }
        return sanitized;
    }

    private String sanitizeOptional(String value) {
        return value == null || value.isBlank() ? null : sanitizeToolText(value);
    }

    private String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private record PromptMetadata(
            String promptCode,
            String structuredOutputSchema
    ) {
    }

    public record RunStart(
            String ownerUserId,
            String taskType,
            String status,
            String inputJson,
            String outputJson,
            String traceId,
            Long latencyMs
    ) {
    }

    public record TraceStep(
            String stepId,
            String agentName,
            String status,
            String summary,
            Long latencyMs,
            String promptVersion
    ) {
    }

    public record TokenUsageEstimate(
            Integer promptTokens,
            Integer completionTokens,
            Double estimatedCost
    ) {

        public Integer totalTokens() {
            return promptTokens + completionTokens;
        }
    }
}
