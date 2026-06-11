package com.learningos.evaluation.dto;

import java.time.Instant;
import java.util.List;

public record EvaluationSetResponse(
        String id,
        String code,
        String version,
        String name,
        String description,
        String type,
        String status,
        String courseId,
        String kbId,
        String promptCode,
        String promptVersion,
        String createdBy,
        int sampleCount,
        List<EvaluationSampleResponse> samples,
        Instant createdAt,
        Instant updatedAt
) {
}
