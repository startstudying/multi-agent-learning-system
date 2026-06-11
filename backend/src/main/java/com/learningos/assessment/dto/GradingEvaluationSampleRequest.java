package com.learningos.assessment.dto;

public record GradingEvaluationSampleRequest(
        String sampleId,
        String questionType,
        String knowledgePointId,
        String rubricVersion,
        Double humanScore,
        Double systemScore,
        String humanGrade,
        String systemGrade,
        String humanWrongCause,
        String systemWrongCause
) {
}
