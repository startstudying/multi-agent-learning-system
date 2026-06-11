package com.learningos.evaluation.dto;

public record EvaluationMetricAggregateResponse(
        double average,
        int sampleCount,
        int runCount
) {
}
