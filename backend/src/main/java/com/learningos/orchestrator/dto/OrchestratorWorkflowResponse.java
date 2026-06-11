package com.learningos.orchestrator.dto;

import java.util.List;

public record OrchestratorWorkflowResponse(
        String workflowId,
        String workflowType,
        String agentTaskId,
        String traceId,
        String status,
        List<OrchestratorWorkflowStepResponse> steps,
        OrchestratorWorkflowStepResponse recentFailedStep,
        OrchestratorWorkflowTraceSummary traceSummary,
        List<String> nextActions,
        String retryOfWorkflowId
) {
}
