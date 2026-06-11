package com.learningos.assessment.dto;

import com.learningos.assessment.domain.WrongCauseCategory;

public record FeedbackDiagnosisResponse(
        WrongCauseCategory wrongCauseCategory,
        String misconception,
        String missingPrerequisite,
        String recommendedRemediation,
        String evidence
) {
}
