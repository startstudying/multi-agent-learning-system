package com.learningos.learning.application;

import java.util.List;

public record LearningPathReplanDecision(
        boolean replanRequired,
        String status,
        List<String> affectedPathIds,
        String reasonSummary,
        String traceId
) {
}
