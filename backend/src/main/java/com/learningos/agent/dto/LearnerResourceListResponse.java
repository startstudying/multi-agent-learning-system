package com.learningos.agent.dto;

import java.util.List;

public record LearnerResourceListResponse(
        String taskId,
        List<LearnerResourceResponse> resources
) {
}
