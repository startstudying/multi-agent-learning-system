package com.learningos.evaluation.dto;

import java.util.Map;

public record PromptVersionComparisonRowResponse(
        String promptVersion,
        int runCount,
        int sampleCount,
        Map<String, EvaluationMetricAggregateResponse> metrics,
        Map<String, Double> deltas
) {
}
