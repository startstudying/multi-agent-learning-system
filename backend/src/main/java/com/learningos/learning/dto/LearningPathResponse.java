package com.learningos.learning.dto;

import java.util.List;

public record LearningPathResponse(
        String pathId,
        String learnerId,
        String goalId,
        String reasonSummary,
        List<LearningPathNodeResponse> nodes,
        String traceId,
        String profileSnapshot
) {
}
