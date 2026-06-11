package com.learningos.assessment.dto;

public record AssessmentRecordDetailResponse(
        String answerId,
        String learnerId,
        String questionId,
        String answer,
        Double score,
        String safetyStatus,
        String gradingResultId,
        String wrongQuestionId,
        String wrongCauseAnalysis,
        String masteryUpdates,
        String resourcePushStrategy,
        String replanRecordId,
        String traceId
) {
}
