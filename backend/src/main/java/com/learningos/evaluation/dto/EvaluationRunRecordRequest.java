package com.learningos.evaluation.dto;

import java.util.List;

public record EvaluationRunRecordRequest(
        String evaluationSetId,
        String promptCode,
        String promptVersion,
        String model,
        String status,
        Integer sampleCount,
        String traceId,
        List<EvaluationMetricRequest> metrics
) {
    public EvaluationRunRecordRequest {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
