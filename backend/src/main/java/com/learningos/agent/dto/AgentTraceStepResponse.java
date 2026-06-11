package com.learningos.agent.dto;

public record AgentTraceStepResponse(
        String stepId,
        String agentName,
        String status,
        String summary,
        long latencyMs,
        String model,
        String promptVersion
) {
}
