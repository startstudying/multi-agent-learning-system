package com.learningos.assessment.dto;

public record WrongQuestionDetailResponse(
        String wrongQuestionId,
        String answerId,
        String gradingResultId,
        String learnerId,
        String questionId,
        String knowledgePointId,
        Double score,
        String wrongCauseAnalysis,
        String resourcePushStrategy,
        String replanRecordId,
        String traceId
) {
}
