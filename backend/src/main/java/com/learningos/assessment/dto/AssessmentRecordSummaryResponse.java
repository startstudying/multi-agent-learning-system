package com.learningos.assessment.dto;

import java.time.Instant;

public record AssessmentRecordSummaryResponse(
        String answerId,
        String learnerId,
        String questionId,
        Double score,
        String safetyStatus,
        String wrongQuestionId,
        String resourcePushStrategy,
        String traceId,
        Instant createdAt
) {
}
