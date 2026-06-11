package com.learningos.rag.application;

import java.util.List;

public record RagEvaluationResult(
        double recallAtK,
        double citationAccuracy,
        double groundedness,
        double noSourceRefusalRate,
        int expectedSourceCount,
        int evaluatedCitationCount,
        int relevantCitationCount,
        BenchmarkSummary benchmarkSummary,
        List<SampleResult> sampleResults,
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
                expectedSourceCount,
                evaluatedCitationCount,
                relevantCitationCount,
                null,
                List.of(),
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
            boolean expectedNoSource,
            boolean noSourceRefusal,
            int expectedChunkCount,
            int evaluatedCitationCount,
            int relevantCitationCount
    ) {
    }
}
