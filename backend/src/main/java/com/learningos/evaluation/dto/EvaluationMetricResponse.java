package com.learningos.evaluation.dto;

public record EvaluationMetricResponse(
        String metricName,
        double metricValue,
        String metricUnit,
        int sampleCount
) {
}
