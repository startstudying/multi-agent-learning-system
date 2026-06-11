package com.learningos.assessment.application;

import java.util.List;

public record GradingEvaluationSummary(
        double meanAbsoluteError,
        double agreementRate,
        double gradeAgreementRate,
        double wrongCauseAgreementRate,
        int sampleCount,
        GroupedAnalysis groupedAnalysis
) {
    public GradingEvaluationSummary(
            double meanAbsoluteError,
            double agreementRate,
            int sampleCount
    ) {
        this(
                meanAbsoluteError,
                agreementRate,
                agreementRate,
                0.0,
                sampleCount,
                GroupedAnalysis.empty()
        );
    }

    public record GroupedAnalysis(
            List<GroupMetrics> questionType,
            List<GroupMetrics> knowledgePointId,
            List<GroupMetrics> rubricVersion
    ) {
        public static GroupedAnalysis empty() {
            return new GroupedAnalysis(List.of(), List.of(), List.of());
        }
    }

    public record GroupMetrics(
            String groupKey,
            int sampleCount,
            double meanAbsoluteError,
            double gradeAgreementRate,
            double wrongCauseAgreementRate
    ) {
    }
}
