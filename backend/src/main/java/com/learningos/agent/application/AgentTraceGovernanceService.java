package com.learningos.agent.application;

import com.learningos.agent.domain.AgentTask;
import com.learningos.agent.domain.AgentToolCall;
import com.learningos.agent.domain.AgentTrace;
import com.learningos.agent.dto.AgentToolCallResponse;
import com.learningos.agent.dto.AgentTraceResponse;
import com.learningos.agent.dto.AgentTraceRetentionPolicyResponse;
import com.learningos.agent.dto.AgentTraceSearchItemResponse;
import com.learningos.agent.dto.AgentTraceSearchResponse;
import com.learningos.agent.dto.AgentTraceStepResponse;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentToolCallRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class AgentTraceGovernanceService {

    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final AgentToolCallRepository agentToolCallRepository;

    public AgentTraceGovernanceService(
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            AgentToolCallRepository agentToolCallRepository
    ) {
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.agentToolCallRepository = agentToolCallRepository;
    }

    @Transactional(readOnly = true)
    public AgentTraceSearchResponse search(
            String currentUserId,
            String userId,
            String agentType,
            String status,
            Instant from,
            Instant to,
            String failureReason
    ) {
        return search(currentUserId, isAdmin(currentUserId), userId, agentType, status, from, to, failureReason);
    }

    @Transactional(readOnly = true)
    public AgentTraceSearchResponse search(
            String currentUserId,
            boolean currentUserAdmin,
            String userId,
            String agentType,
            String status,
            Instant from,
            Instant to,
            String failureReason
    ) {
        if (!currentUserAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Agent trace search requires admin access");
        }
        List<AgentTraceSearchItemResponse> items = agentTaskRepository.findAll().stream()
                .filter(task -> matches(userId, task.getOwnerUserId()))
                .filter(task -> matches(agentType, task.getTaskType()))
                .filter(task -> matches(status, task.getStatus()))
                .filter(task -> from == null || !task.getCreatedAt().isBefore(from))
                .filter(task -> to == null || !task.getCreatedAt().isAfter(to))
                .filter(task -> failureReason == null || failureReason.isBlank()
                        || safe(task.getOutputJson()).contains(failureReason.trim()))
                .sorted(Comparator.comparing(AgentTask::getCreatedAt).reversed())
                .map(task -> new AgentTraceSearchItemResponse(
                        task.getId(),
                        task.getTraceId(),
                        task.getOwnerUserId(),
                        task.getTaskType(),
                        task.getStatus(),
                        failureReason(task),
                        Math.toIntExact(agentTraceRepository.countByAgentTaskId(task.getId())),
                        task.getCreatedAt()
                ))
                .toList();
        return new AgentTraceSearchResponse(items);
    }

    @Transactional(readOnly = true)
    public AgentTraceResponse getTrace(String currentUserId, String taskId) {
        return getTrace(currentUserId, isAdmin(currentUserId), taskId);
    }

    @Transactional(readOnly = true)
    public AgentTraceResponse getTrace(String currentUserId, boolean currentUserAdmin, String taskId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> scopedMissing(currentUserAdmin));
        if (!currentUserAdmin && !currentUserId.equals(task.getOwnerUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Agent trace access denied");
        }
        List<AgentTraceStepResponse> steps = agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getId())
                .stream()
                .map(this::toTraceStepResponse)
                .toList();
        List<AgentToolCallResponse> toolCalls = agentToolCallRepository.findByAgentTaskIdOrderByCreatedAtAsc(task.getId())
                .stream()
                .map(this::toToolCallResponse)
                .toList();
        return new AgentTraceResponse(
                task.getId(),
                task.getStatus(),
                steps,
                task.getTraceId(),
                toolCalls,
                AgentTraceRetentionPolicyResponse.standard()
        );
    }

    private AgentTraceStepResponse toTraceStepResponse(AgentTrace trace) {
        return new AgentTraceStepResponse(
                trace.getStepId(),
                trace.getAgentName(),
                trace.getStatus(),
                trace.getSummary(),
                trace.getLatencyMs(),
                trace.getModel(),
                trace.getPromptVersion()
        );
    }

    private AgentToolCallResponse toToolCallResponse(AgentToolCall toolCall) {
        return new AgentToolCallResponse(
                toolCall.getToolName(),
                toolCall.getStatus(),
                toolCall.getInputSummary(),
                toolCall.getOutputSummary(),
                toolCall.getErrorMessage(),
                toolCall.getLatencyMs()
        );
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.trim().equals(actual);
    }

    private String failureReason(AgentTask task) {
        return AgentRuntimeConstants.STATUS_FAILED.equals(task.getStatus())
                && safe(task.getOutputJson()).contains("MODEL_PROVIDER_ERROR")
                ? "MODEL_PROVIDER_ERROR"
                : null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private ApiException scopedMissing(String currentUserId) {
        return scopedMissing(isAdmin(currentUserId));
    }

    private ApiException scopedMissing(boolean currentUserAdmin) {
        return currentUserAdmin
                ? new ApiException(ErrorCode.NOT_FOUND, "Agent trace not found")
                : new ApiException(ErrorCode.FORBIDDEN, "Agent trace access denied");
    }

    private boolean isAdmin(String currentUserId) {
        return "admin".equals(currentUserId);
    }
}
