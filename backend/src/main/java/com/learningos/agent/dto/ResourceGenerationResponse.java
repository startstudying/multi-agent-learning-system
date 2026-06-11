package com.learningos.agent.dto;

import java.time.Instant;
import java.util.List;

public record ResourceGenerationResponse(
        String taskId,
        String agentTaskId,
        String status,
        String reviewStatus,
        int progressPercent,
        String safetyStatus,
        List<GeneratedResourceResponse> resources,
        String traceId,
        String profileSnapshot,
        int retryCount,
        Instant nextRetryAt,
        String lastError,
        boolean recoverable
) {
}
