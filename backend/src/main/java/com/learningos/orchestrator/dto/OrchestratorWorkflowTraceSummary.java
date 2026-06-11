package com.learningos.orchestrator.dto;

public record OrchestratorWorkflowTraceSummary(
        String traceId,
        String agentTaskId,
        int totalSteps,
        int failedSteps,
        String lastStepId,
        String lastStatus
) {
}
