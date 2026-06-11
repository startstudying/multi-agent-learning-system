package com.learningos.agent.dto;

import java.util.List;

public record AgentTraceRetentionPolicyResponse(
        int auditRetentionDays,
        int textRetentionDays,
        List<String> longTermAuditFields,
        List<String> cleanableTextFields
) {
    public static AgentTraceRetentionPolicyResponse standard() {
        return new AgentTraceRetentionPolicyResponse(
                365,
                30,
                List.of("taskId", "traceId", "userId", "agentType", "status", "createdAt"),
                List.of("agent_task.inputJson", "agent_task.outputJson", "agent_trace.summary", "agent_tool_call.inputJson", "agent_tool_call.outputJson")
        );
    }
}
