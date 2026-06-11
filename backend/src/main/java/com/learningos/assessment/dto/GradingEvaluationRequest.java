package com.learningos.assessment.dto;

import java.util.List;

public record GradingEvaluationRequest(
        String courseId,
        List<GradingEvaluationSampleRequest> samples,
        List<Double> humanScores,
        List<Double> aiScores,
        Double agreementThreshold
) {
    public GradingEvaluationRequest(List<GradingEvaluationSampleRequest> samples) {
        this(null, samples, null, null, null);
    }

    public GradingEvaluationRequest(String courseId, List<GradingEvaluationSampleRequest> samples) {
        this(courseId, samples, null, null, null);
    }

    public GradingEvaluationRequest(
            List<Double> humanScores,
            List<Double> aiScores,
            Double agreementThreshold
    ) {
        this(null, null, humanScores, aiScores, agreementThreshold);
    }

    public GradingEvaluationRequest(
            String courseId,
            List<Double> humanScores,
            List<Double> aiScores,
            Double agreementThreshold
    ) {
        this(courseId, null, humanScores, aiScores, agreementThreshold);
    }

    public double normalizedAgreementThreshold() {
        return agreementThreshold == null ? 0.05 : agreementThreshold;
    }

    public boolean hasLegacyScores() {
        return humanScores != null || aiScores != null;
    }
}
