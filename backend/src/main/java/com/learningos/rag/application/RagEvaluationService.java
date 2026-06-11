package com.learningos.rag.application;

import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RagEvaluationService {

    public RagEvaluationResult evaluate(RagEvaluationRequest request) {
        RagEvaluationRequest safeRequest = request == null
                ? new RagEvaluationRequest(List.of(), List.of(), null)
                : request;
        if (safeRequest.benchmark() != null && !safeRequest.benchmark().samples().isEmpty()) {
            return evaluateBenchmark(safeRequest.benchmark());
        }

        Set<String> expectedSourceIds = normalizedExpectedSourceIds(safeRequest.expectedSourceIds());
        List<SourceCitation> actualCitations = safeRequest.actualCitations();
        int topK = normalizedTopK(safeRequest.topK(), actualCitations.size());

        long topKHits = actualCitations.stream()
                .limit(topK)
                .map(SourceCitation::documentId)
                .filter(expectedSourceIds::contains)
                .collect(LinkedHashSet::new, Set::add, Set::addAll)
                .size();
        long relevantCitations = actualCitations.stream()
                .map(SourceCitation::documentId)
                .filter(expectedSourceIds::contains)
                .count();

        double recallAtK = expectedSourceIds.isEmpty()
                ? (actualCitations.isEmpty() ? 1.0 : 0.0)
                : (double) topKHits / expectedSourceIds.size();
        double citationAccuracy = actualCitations.isEmpty()
                ? (expectedSourceIds.isEmpty() ? 1.0 : 0.0)
                : (double) relevantCitations / actualCitations.size();
        double groundedness = harmonicMean(recallAtK, citationAccuracy);

        return new RagEvaluationResult(
                recallAtK,
                citationAccuracy,
                groundedness,
                expectedSourceIds.size(),
                actualCitations.size(),
                Math.toIntExact(relevantCitations)
        );
    }

    private RagEvaluationResult evaluateBenchmark(RagEvaluationRequest.Benchmark benchmark) {
        List<RagEvaluationResult.SampleResult> sampleResults = new ArrayList<>();
        int expectedSourceCount = 0;
        int evaluatedCitationCount = 0;
        int relevantCitationCount = 0;
        int noSourceSampleCount = 0;
        int noSourceRefusalCount = 0;

        double recallTotal = 0.0;
        double citationAccuracyTotal = 0.0;
        int sourceRequiredSampleCount = 0;

        for (RagEvaluationRequest.BenchmarkSample sample : benchmark.samples()) {
            Set<String> expectedChunkIds = normalizedExpectedSourceIds(sample.expectedChunkIds());
            List<SourceCitation> citations = sample.actualCitations();
            int topK = normalizedTopK(sample.topK() == null ? benchmark.topK() : sample.topK(), citations.size());
            boolean expectedNoSource = Boolean.TRUE.equals(sample.expectedNoSource());

            long topKHits = citations.stream()
                    .limit(topK)
                    .map(SourceCitation::documentId)
                    .filter(expectedChunkIds::contains)
                    .collect(LinkedHashSet::new, Set::add, Set::addAll)
                    .size();
            long sampleRelevantCitations = citations.stream()
                    .map(SourceCitation::documentId)
                    .filter(expectedChunkIds::contains)
                    .count();

            double recallAtK = expectedChunkIds.isEmpty()
                    ? (citations.isEmpty() ? 1.0 : 0.0)
                    : (double) topKHits / expectedChunkIds.size();
            double citationAccuracy = citations.isEmpty()
                    ? (expectedChunkIds.isEmpty() ? 1.0 : 0.0)
                    : (double) sampleRelevantCitations / citations.size();
            double groundedness = harmonicMean(recallAtK, citationAccuracy);
            boolean noSourceRefusal = expectedNoSource && isNoSourceRefusal(sample.actualAnswer(), citations);

            if (expectedNoSource) {
                noSourceSampleCount++;
                if (noSourceRefusal) {
                    noSourceRefusalCount++;
                }
            } else {
                sourceRequiredSampleCount++;
                recallTotal += recallAtK;
                citationAccuracyTotal += citationAccuracy;
                expectedSourceCount += expectedChunkIds.size();
                evaluatedCitationCount += citations.size();
                relevantCitationCount += Math.toIntExact(sampleRelevantCitations);
            }

            sampleResults.add(new RagEvaluationResult.SampleResult(
                    sample.sampleKey(),
                    recallAtK,
                    citationAccuracy,
                    groundedness,
                    expectedNoSource,
                    noSourceRefusal,
                    expectedChunkIds.size(),
                    citations.size(),
                    Math.toIntExact(sampleRelevantCitations)
            ));
        }

        double recallAtK = sourceRequiredSampleCount == 0 ? 1.0 : recallTotal / sourceRequiredSampleCount;
        double citationAccuracy = sourceRequiredSampleCount == 0 ? 1.0 : citationAccuracyTotal / sourceRequiredSampleCount;
        double groundedness = harmonicMean(recallAtK, citationAccuracy);
        double noSourceRefusalRate = noSourceSampleCount == 0 ? 1.0 : (double) noSourceRefusalCount / noSourceSampleCount;
        int topK = benchmark.topK() == null ? 0 : benchmark.topK();

        RagEvaluationResult.BenchmarkSummary summary = new RagEvaluationResult.BenchmarkSummary(
                benchmark.benchmarkId(),
                benchmark.courseId(),
                benchmark.name(),
                benchmark.version(),
                benchmark.samples().size(),
                noSourceSampleCount,
                sourceRequiredSampleCount,
                topK
        );
        return new RagEvaluationResult(
                recallAtK,
                citationAccuracy,
                groundedness,
                noSourceRefusalRate,
                expectedSourceCount,
                evaluatedCitationCount,
                relevantCitationCount,
                summary,
                List.copyOf(sampleResults),
                report(summary, recallAtK, citationAccuracy, groundedness, noSourceRefusalRate)
        );
    }

    private boolean isNoSourceRefusal(String answer, List<SourceCitation> citations) {
        if (!citations.isEmpty() || answer == null) {
            return false;
        }
        String normalized = answer.trim().toUpperCase();
        return normalized.contains("NO_SOURCE")
                || normalized.contains("NO SOURCE")
                || normalized.contains("NO CITED COURSE MATERIAL")
                || normalized.contains("NO CITED MATERIAL");
    }

    private String report(
            RagEvaluationResult.BenchmarkSummary summary,
            double recallAtK,
            double citationAccuracy,
            double groundedness,
            double noSourceRefusalRate
    ) {
        String title = summary.name() == null || summary.name().isBlank()
                ? summary.benchmarkId()
                : summary.name();
        return """
                RAG Quality Evaluation Report
                Benchmark: %s
                Version: %s
                Recall@K: %.6f
                Citation Accuracy: %.6f
                Groundedness: %.6f
                No-source Refusal Rate: %.6f
                """.formatted(
                title,
                summary.version() == null ? "" : summary.version(),
                recallAtK,
                citationAccuracy,
                groundedness,
                noSourceRefusalRate
        );
    }

    private Set<String> normalizedExpectedSourceIds(List<String> sourceIds) {
        Set<String> normalized = new LinkedHashSet<>();
        sourceIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(sourceId -> !sourceId.isEmpty())
                .forEach(normalized::add);
        return normalized;
    }

    private int normalizedTopK(Integer requestedTopK, int citationCount) {
        if (requestedTopK == null || requestedTopK <= 0) {
            return citationCount;
        }
        return Math.min(requestedTopK, citationCount);
    }

    private double harmonicMean(double first, double second) {
        if (first == 0.0 || second == 0.0) {
            return 0.0;
        }
        return 2.0 * first * second / (first + second);
    }
}
