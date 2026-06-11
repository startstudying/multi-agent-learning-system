package com.learningos.assessment.dto;

import java.time.Instant;

public record WrongQuestionSummaryResponse(
        String wrongQuestionId,
        String answerId,
        String learnerId,
        String questionId,
        String knowledgePointId,
        Double score,
        String resourcePushStrategy,
        String traceId,
        Instant createdAt
) {
}
