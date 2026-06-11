package com.learningos.agent.dto;

import java.util.List;

public record AgentTraceSearchResponse(
        List<AgentTraceSearchItemResponse> items
) {
}
