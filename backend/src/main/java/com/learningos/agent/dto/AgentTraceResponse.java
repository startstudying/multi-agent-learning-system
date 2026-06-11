package com.learningos.agent.dto;

import java.util.List;

public record AgentTraceResponse(
        String taskId,
        String status,
        List<AgentTraceStepResponse> steps,
        String traceId,
        List<AgentToolCallResponse> toolCalls,
        AgentTraceRetentionPolicyResponse retentionPolicy
) {
    public AgentTraceResponse(
            String taskId,
            String status,
            List<AgentTraceStepResponse> steps,
            String traceId
    ) {
        this(taskId, status, steps, traceId, List.of(), AgentTraceRetentionPolicyResponse.standard());
    }
}
