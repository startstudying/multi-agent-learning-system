package com.learningos.evaluation.dto;

public record EvaluationMetricRequest(
        String metricName,
        Double metricValue,
        String metricUnit,
        Integer sampleCount
) {
}
