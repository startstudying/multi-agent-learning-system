package com.learningos.assessment.application;

import com.learningos.assessment.dto.FeedbackDiagnosisResponse;
import com.learningos.assessment.dto.MasteryUpdateResponse;

import java.util.List;

public record AssessmentFeedbackEvaluation(
        double score,
        List<MasteryUpdateResponse> masteryUpdates,
        FeedbackDiagnosisResponse feedbackDiagnosis,
        String wrongCauseAnalysis,
        String resourcePushStrategy,
        boolean replanTriggered
) {
}
