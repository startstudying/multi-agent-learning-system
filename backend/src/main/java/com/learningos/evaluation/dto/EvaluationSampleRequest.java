package com.learningos.evaluation.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record EvaluationSampleRequest(
        @Size(max = 120) String sampleKey,
        @Size(max = 4000) String question,
        List<String> expectedSourceIds,
        Integer topK,
        @Size(max = 120) String questionId,
        @Size(max = 8000) String answerText,
        @Size(max = 4000) String rubric,
        Double humanScore,
        @Size(max = 8000) String learnerProfileSnapshot,
        @Size(max = 1000) String learningGoal,
        @Size(max = 120) String expectedResourceType,
        List<String> qualityCriteria
) {
    public EvaluationSampleRequest {
        expectedSourceIds = expectedSourceIds == null ? List.of() : List.copyOf(expectedSourceIds);
        qualityCriteria = qualityCriteria == null ? List.of() : List.copyOf(qualityCriteria);
    }
}
