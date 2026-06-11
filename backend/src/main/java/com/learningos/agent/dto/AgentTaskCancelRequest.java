package com.learningos.agent.dto;

import jakarta.validation.constraints.Size;

public record AgentTaskCancelRequest(
        @Size(max = 500) String reason
) {
}
