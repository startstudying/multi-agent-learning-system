package com.learningos.evaluation.dto;

import java.util.List;
import java.util.Map;

public record PromptVersionQualityComparisonResponse(
        String evaluationSetId,
        String promptCode,
        String baselinePromptVersion,
        List<PromptVersionComparisonRowResponse> rows,
        Map<String, String> winnerByMetric
) {
}
