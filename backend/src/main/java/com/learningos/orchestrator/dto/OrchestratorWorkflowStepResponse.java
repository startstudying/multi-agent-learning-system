package com.learningos.orchestrator.dto;

public record OrchestratorWorkflowStepResponse(
        String stepId,
        String agentName,
        String status,
        String summary,
        Long latencyMs,
        String model,
        String promptVersion,
        Integer sequenceNo,
        String inputDto,
        String outputDto,
        String failurePolicy,
        String retryPolicy,
        Boolean retryable
) {
}
