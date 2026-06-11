package com.learningos.orchestrator.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkflowRequest(
        @NotBlank String workflowType,
        @NotBlank String learnerId,
        String payloadJson,
        String requestId,
        String retryOfWorkflowId
) {

    public CreateWorkflowRequest(
            String workflowType,
            String learnerId,
            String payloadJson,
            String requestId
    ) {
        this(workflowType, learnerId, payloadJson, requestId, null);
    }
}
