package com.learningos.assessment.dto;

import java.util.List;

public record AnswerSubmitResponse(
        String answerId,
        String gradingResultId,
        double score,
        List<MasteryUpdateResponse> masteryUpdates,
        String feedbackId,
        boolean replanTriggered,
        String replanRecordId,
        ReplanDecisionResponse replanDecision,
        String wrongCauseAnalysis,
        FeedbackDiagnosisResponse feedbackDiagnosis,
        String resourcePushStrategy,
        String traceId
) {
}
