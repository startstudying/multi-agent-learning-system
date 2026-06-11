package com.learningos.agent.application;

public record AgentExecutionContext(
        String agentTaskId,
        String traceId
) {
}
