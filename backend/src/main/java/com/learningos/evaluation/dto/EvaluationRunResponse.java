package com.learningos.evaluation.dto;

import java.time.Instant;
import java.util.List;

public record EvaluationRunResponse(
        String id,
        String evaluationSetId,
        String setType,
        String promptCode,
        String promptVersion,
        String model,
        String status,
        int sampleCount,
        String traceId,
        List<EvaluationMetricResponse> metrics,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {
}
