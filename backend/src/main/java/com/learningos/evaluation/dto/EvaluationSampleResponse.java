package com.learningos.evaluation.dto;

import java.util.List;

public record EvaluationSampleResponse(
        String id,
        String sampleKey,
        String question,
        List<String> expectedSourceIds,
        Integer topK,
        String questionId,
        String answerText,
        String rubric,
        Double humanScore,
        String learnerProfileSnapshot,
        String learningGoal,
        String expectedResourceType,
        List<String> qualityCriteria
) {
}
