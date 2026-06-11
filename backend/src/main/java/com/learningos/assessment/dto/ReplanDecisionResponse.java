package com.learningos.assessment.dto;

import java.util.List;

public record ReplanDecisionResponse(
        String status,
        boolean replanRequired,
        List<String> affectedPathIds,
        String reasonSummary,
        String traceId
) {
}
