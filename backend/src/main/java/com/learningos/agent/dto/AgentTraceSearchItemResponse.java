package com.learningos.agent.dto;

import java.time.Instant;

public record AgentTraceSearchItemResponse(
        String taskId,
        String traceId,
        String userId,
        String agentType,
        String status,
        String failureReason,
        int stepCount,
        Instant createdAt
) {
}
