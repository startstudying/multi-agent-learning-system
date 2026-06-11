package com.learningos.agent.dto;

public record AgentToolCallResponse(
        String toolName,
        String status,
        String inputSummary,
        String outputSummary,
        String errorMessage,
        Long latencyMs
) {
}
