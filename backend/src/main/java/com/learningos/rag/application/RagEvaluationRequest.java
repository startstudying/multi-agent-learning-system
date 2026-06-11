package com.learningos.rag.application;

import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;

import java.util.List;

public record RagEvaluationRequest(
        List<String> expectedSourceIds,
        List<SourceCitation> actualCitations,
        Integer topK,
        Benchmark benchmark
) {
    public RagEvaluationRequest {
        expectedSourceIds = expectedSourceIds == null ? List.of() : List.copyOf(expectedSourceIds);
        actualCitations = actualCitations == null ? List.of() : List.copyOf(actualCitations);
    }

    public RagEvaluationRequest(
            List<String> expectedSourceIds,
            List<SourceCitation> actualCitations,
            Integer topK
    ) {
        this(expectedSourceIds, actualCitations, topK, null);
    }

    public record Benchmark(
            String benchmarkId,
            String courseId,
            String name,
            String version,
            Integer topK,
            List<BenchmarkSample> samples
    ) {
        public Benchmark {
            samples = samples == null ? List.of() : List.copyOf(samples);
        }
    }

    public record BenchmarkSample(
            String sampleKey,
            String question,
            List<String> expectedChunkIds,
            String expectedAnswer,
            String forbiddenAnswerScope,
            Boolean expectedNoSource,
            String actualAnswer,
            List<SourceCitation> actualCitations,
            Integer topK
    ) {
        public BenchmarkSample {
            expectedChunkIds = expectedChunkIds == null ? List.of() : List.copyOf(expectedChunkIds);
            actualCitations = actualCitations == null ? List.of() : List.copyOf(actualCitations);
        }
    }
}
