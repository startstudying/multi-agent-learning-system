package com.learningos.assessment.dto;

public record MasteryUpdateResponse(
        String knowledgePointId,
        double beforeMastery,
        double afterMastery,
        String reasonSummary
) {
}
