package com.learningos.rag.application;

import java.util.List;
import java.util.Map;

public record RagEvaluationResult(
        double recallAtK,
        double citationAccuracy,
        double groundedness,
        double noSourceRefusalRate,
        double coreClaimCitationCoverage,
        double uncitedContextLeakRate,
        int expectedSourceCount,
        int evaluatedCitationCount,
        int relevantCitationCount,
        BenchmarkSummary benchmarkSummary,
        List<SampleResult> sampleResults,
        ComparisonResult comparisonResult,
        String report
) {
    public RagEvaluationResult(
            double recallAtK,
            double citationAccuracy,
            double groundedness,
            int expectedSourceCount,
            int evaluatedCitationCount,
            int relevantCitationCount
    ) {
        this(
                recallAtK,
                citationAccuracy,
                groundedness,
                0.0,
                1.0,
                0.0,
                expectedSourceCount,
                evaluatedCitationCount,
                relevantCitationCount,
                null,
                List.of(),
                null,
                null
        );
    }

    public record BenchmarkSummary(
            String benchmarkId,
            String courseId,
            String name,
            String version,
            int sampleCount,
            int noSourceSampleCount,
            int sourceRequiredSampleCount,
            int topK
    ) {
    }

    public record SampleResult(
            String sampleKey,
            double recallAtK,
            double citationAccuracy,
            double groundedness,
            double coreClaimCitationCoverage,
            boolean uncitedContextLeak,
            boolean expectedNoSource,
            boolean noSourceRefusal,
            int expectedChunkCount,
            int evaluatedCitationCount,
            int relevantCitationCount
    ) {
    }

    public record StrategyMetrics(
            double recallAtK,
            double citationAccuracy,
            double groundedness,
            double noSourceRefusalRate,
            double coreClaimCitationCoverage,
            double uncitedContextLeakRate,
            int expectedSourceCount,
            int evaluatedCitationCount,
            int relevantCitationCount
    ) {
    }

    public record ComparisonResult(
            String comparisonId,
            String baselineId,
            String candidateId,
            StrategyMetrics baselineMetrics,
            StrategyMetrics candidateMetrics,
            Map<String, Double> deltas,
            Map<String, String> winnerByMetric,
            List<PairedSampleResult> sampleResults
    ) {
        public ComparisonResult {
            deltas = deltas == null ? Map.of() : Map.copyOf(deltas);
            winnerByMetric = winnerByMetric == null ? Map.of() : Map.copyOf(winnerByMetric);
            sampleResults = sampleResults == null ? List.of() : List.copyOf(sampleResults);
        }
    }

    public record PairedSampleResult(
            String sampleKey,
            SampleResult baseline,
            SampleResult candidate,
            Map<String, Double> deltas,
            Map<String, String> winnerByMetric
    ) {
        public PairedSampleResult {
            deltas = deltas == null ? Map.of() : Map.copyOf(deltas);
            winnerByMetric = winnerByMetric == null ? Map.of() : Map.copyOf(winnerByMetric);
        }
    }
}
