package com.learningos.rag.application;

import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RagEvaluationService {

    private final CitationCoverageAnalyzer citationCoverageAnalyzer = new CitationCoverageAnalyzer();

    public RagEvaluationResult evaluate(RagEvaluationRequest request) {
        RagEvaluationRequest safeRequest = request == null
                ? new RagEvaluationRequest(List.of(), List.of(), null)
                : request;
        if (safeRequest.comparison() != null && !safeRequest.comparison().samples().isEmpty()) {
            return evaluateComparison(safeRequest.comparison());
        }
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
        MetricsAccumulator accumulator = new MetricsAccumulator();

        for (RagEvaluationRequest.BenchmarkSample sample : benchmark.samples()) {
            SampleEvaluation evaluation = evaluateSample(
                    sample.sampleKey(),
                    sample.expectedChunkIds(),
                    sample.expectedNoSource(),
                    sample.actualAnswer(),
                    sample.actualCitations(),
                    sample.topK() == null ? benchmark.topK() : sample.topK()
            );
            accumulator.add(evaluation);
            sampleResults.add(evaluation.result());
        }

        RagEvaluationResult.StrategyMetrics metrics = accumulator.toMetrics();
        int topK = benchmark.topK() == null ? 0 : benchmark.topK();

        RagEvaluationResult.BenchmarkSummary summary = new RagEvaluationResult.BenchmarkSummary(
                benchmark.benchmarkId(),
                benchmark.courseId(),
                benchmark.name(),
                benchmark.version(),
                benchmark.samples().size(),
                accumulator.noSourceSampleCount,
                accumulator.sourceRequiredSampleCount,
                topK
        );
        return new RagEvaluationResult(
                metrics.recallAtK(),
                metrics.citationAccuracy(),
                metrics.groundedness(),
                metrics.noSourceRefusalRate(),
                metrics.coreClaimCitationCoverage(),
                metrics.uncitedContextLeakRate(),
                metrics.expectedSourceCount(),
                metrics.evaluatedCitationCount(),
                metrics.relevantCitationCount(),
                summary,
                List.copyOf(sampleResults),
                null,
                report(summary, metrics)
        );
    }

    private RagEvaluationResult evaluateComparison(RagEvaluationRequest.Comparison comparison) {
        MetricsAccumulator baseline = new MetricsAccumulator();
        MetricsAccumulator candidate = new MetricsAccumulator();
        List<RagEvaluationResult.PairedSampleResult> pairedResults = new ArrayList<>();
        List<RagEvaluationResult.SampleResult> candidateSamples = new ArrayList<>();

        for (RagEvaluationRequest.ComparisonSample sample : comparison.samples()) {
            int topK = sample.topK() == null ? comparison.topK() == null ? 0 : comparison.topK() : sample.topK();
            RagEvaluationRequest.ComparisonOutput baselineOutput = sample.baseline() == null
                    ? new RagEvaluationRequest.ComparisonOutput(null, List.of())
                    : sample.baseline();
            RagEvaluationRequest.ComparisonOutput candidateOutput = sample.candidate() == null
                    ? new RagEvaluationRequest.ComparisonOutput(null, List.of())
                    : sample.candidate();
            SampleEvaluation baselineEvaluation = evaluateSample(
                    sample.sampleKey(),
                    sample.expectedChunkIds(),
                    sample.expectedNoSource(),
                    baselineOutput.answer(),
                    baselineOutput.citations(),
                    topK
            );
            SampleEvaluation candidateEvaluation = evaluateSample(
                    sample.sampleKey(),
                    sample.expectedChunkIds(),
                    sample.expectedNoSource(),
                    candidateOutput.answer(),
                    candidateOutput.citations(),
                    topK
            );
            baseline.add(baselineEvaluation);
            candidate.add(candidateEvaluation);
            candidateSamples.add(candidateEvaluation.result());
            pairedResults.add(new RagEvaluationResult.PairedSampleResult(
                    sample.sampleKey(),
                    baselineEvaluation.result(),
                    candidateEvaluation.result(),
                    sampleDeltas(baselineEvaluation.result(), candidateEvaluation.result()),
                    sampleWinnerByMetric(comparison.baselineId(), comparison.candidateId(), baselineEvaluation.result(), candidateEvaluation.result())
            ));
        }

        RagEvaluationResult.StrategyMetrics baselineMetrics = baseline.toMetrics();
        RagEvaluationResult.StrategyMetrics candidateMetrics = candidate.toMetrics();
        Map<String, Double> deltas = metricDeltas(baselineMetrics, candidateMetrics);
        Map<String, String> winnerByMetric = winnerByMetric(comparison.baselineId(), comparison.candidateId(), baselineMetrics, candidateMetrics);
        RagEvaluationResult.ComparisonResult comparisonResult = new RagEvaluationResult.ComparisonResult(
                comparison.comparisonId(),
                comparison.baselineId(),
                comparison.candidateId(),
                baselineMetrics,
                candidateMetrics,
                deltas,
                winnerByMetric,
                pairedResults
        );
        return new RagEvaluationResult(
                candidateMetrics.recallAtK(),
                candidateMetrics.citationAccuracy(),
                candidateMetrics.groundedness(),
                candidateMetrics.noSourceRefusalRate(),
                candidateMetrics.coreClaimCitationCoverage(),
                candidateMetrics.uncitedContextLeakRate(),
                candidateMetrics.expectedSourceCount(),
                candidateMetrics.evaluatedCitationCount(),
                candidateMetrics.relevantCitationCount(),
                null,
                List.copyOf(candidateSamples),
                comparisonResult,
                comparisonReport(comparison, baselineMetrics, candidateMetrics, deltas)
        );
    }

    private SampleEvaluation evaluateSample(
            String sampleKey,
            List<String> expectedChunkIds,
            Boolean expectedNoSource,
            String actualAnswer,
            List<SourceCitation> actualCitations,
            Integer topK
    ) {
        Set<String> expectedIds = normalizedExpectedSourceIds(expectedChunkIds);
        List<SourceCitation> citations = actualCitations == null ? List.of() : actualCitations;
        int normalizedTopK = normalizedTopK(topK, citations.size());
        boolean noSourceExpected = Boolean.TRUE.equals(expectedNoSource);

        long topKHits = citations.stream()
                .limit(normalizedTopK)
                .map(SourceCitation::documentId)
                .filter(expectedIds::contains)
                .collect(LinkedHashSet::new, Set::add, Set::addAll)
                .size();
        long relevantCitations = citations.stream()
                .map(SourceCitation::documentId)
                .filter(expectedIds::contains)
                .count();
        double recallAtK = expectedIds.isEmpty()
                ? (citations.isEmpty() ? 1.0 : 0.0)
                : (double) topKHits / expectedIds.size();
        double citationAccuracy = citations.isEmpty()
                ? (expectedIds.isEmpty() ? 1.0 : 0.0)
                : (double) relevantCitations / citations.size();
        double groundedness = harmonicMean(recallAtK, citationAccuracy);
        boolean noSourceRefusal = noSourceExpected && isNoSourceRefusal(actualAnswer, citations);
        CitationCoverageAnalyzer.CoverageResult coverage = citationCoverageAnalyzer.analyze(actualAnswer, citations);
        boolean leak = !noSourceExpected && coverage.uncitedContextLeak();
        RagEvaluationResult.SampleResult result = new RagEvaluationResult.SampleResult(
                sampleKey,
                recallAtK,
                citationAccuracy,
                groundedness,
                coverage.coreClaimCitationCoverage(),
                leak,
                noSourceExpected,
                noSourceRefusal,
                expectedIds.size(),
                citations.size(),
                Math.toIntExact(relevantCitations)
        );
        return new SampleEvaluation(result);
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
            RagEvaluationResult.StrategyMetrics metrics
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
                Core Claim Citation Coverage: %.6f
                Uncited Context Leak Rate: %.6f
                """.formatted(
                title,
                summary.version() == null ? "" : summary.version(),
                metrics.recallAtK(),
                metrics.citationAccuracy(),
                metrics.groundedness(),
                metrics.noSourceRefusalRate(),
                metrics.coreClaimCitationCoverage(),
                metrics.uncitedContextLeakRate()
        );
    }

    private String comparisonReport(
            RagEvaluationRequest.Comparison comparison,
            RagEvaluationResult.StrategyMetrics baseline,
            RagEvaluationResult.StrategyMetrics candidate,
            Map<String, Double> deltas
    ) {
        return """
                Fusion RAG POC Comparison
                Comparison: %s
                Baseline: %s
                Candidate: %s
                Baseline Recall@K: %.6f
                Candidate Recall@K: %.6f
                Delta Recall@K: %.6f
                Candidate Core Claim Citation Coverage: %.6f
                Candidate Uncited Context Leak Rate: %.6f
                """.formatted(
                comparison.comparisonId() == null ? "" : comparison.comparisonId(),
                comparison.baselineId() == null ? "" : comparison.baselineId(),
                comparison.candidateId() == null ? "" : comparison.candidateId(),
                baseline.recallAtK(),
                candidate.recallAtK(),
                deltas.getOrDefault("recallAtK", 0.0),
                candidate.coreClaimCitationCoverage(),
                candidate.uncitedContextLeakRate()
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

    private Map<String, Double> metricDeltas(
            RagEvaluationResult.StrategyMetrics baseline,
            RagEvaluationResult.StrategyMetrics candidate
    ) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("recallAtK", round(candidate.recallAtK() - baseline.recallAtK()));
        result.put("citationAccuracy", round(candidate.citationAccuracy() - baseline.citationAccuracy()));
        result.put("groundedness", round(candidate.groundedness() - baseline.groundedness()));
        result.put("noSourceRefusalRate", round(candidate.noSourceRefusalRate() - baseline.noSourceRefusalRate()));
        result.put("coreClaimCitationCoverage", round(candidate.coreClaimCitationCoverage() - baseline.coreClaimCitationCoverage()));
        result.put("uncitedContextLeakRate", round(candidate.uncitedContextLeakRate() - baseline.uncitedContextLeakRate()));
        return result;
    }

    private Map<String, Double> sampleDeltas(
            RagEvaluationResult.SampleResult baseline,
            RagEvaluationResult.SampleResult candidate
    ) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("recallAtK", round(candidate.recallAtK() - baseline.recallAtK()));
        result.put("citationAccuracy", round(candidate.citationAccuracy() - baseline.citationAccuracy()));
        result.put("groundedness", round(candidate.groundedness() - baseline.groundedness()));
        result.put("coreClaimCitationCoverage", round(candidate.coreClaimCitationCoverage() - baseline.coreClaimCitationCoverage()));
        result.put("uncitedContextLeak", candidate.uncitedContextLeak() == baseline.uncitedContextLeak()
                ? 0.0
                : candidate.uncitedContextLeak() ? 1.0 : -1.0);
        return result;
    }

    private Map<String, String> winnerByMetric(
            String baselineId,
            String candidateId,
            RagEvaluationResult.StrategyMetrics baseline,
            RagEvaluationResult.StrategyMetrics candidate
    ) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("recallAtK", winner(baselineId, candidateId, baseline.recallAtK(), candidate.recallAtK(), false));
        result.put("citationAccuracy", winner(baselineId, candidateId, baseline.citationAccuracy(), candidate.citationAccuracy(), false));
        result.put("groundedness", winner(baselineId, candidateId, baseline.groundedness(), candidate.groundedness(), false));
        result.put("noSourceRefusalRate", winner(baselineId, candidateId, baseline.noSourceRefusalRate(), candidate.noSourceRefusalRate(), false));
        result.put("coreClaimCitationCoverage", winner(baselineId, candidateId, baseline.coreClaimCitationCoverage(), candidate.coreClaimCitationCoverage(), false));
        result.put("uncitedContextLeakRate", winner(baselineId, candidateId, baseline.uncitedContextLeakRate(), candidate.uncitedContextLeakRate(), true));
        return result;
    }

    private Map<String, String> sampleWinnerByMetric(
            String baselineId,
            String candidateId,
            RagEvaluationResult.SampleResult baseline,
            RagEvaluationResult.SampleResult candidate
    ) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("recallAtK", winner(baselineId, candidateId, baseline.recallAtK(), candidate.recallAtK(), false));
        result.put("citationAccuracy", winner(baselineId, candidateId, baseline.citationAccuracy(), candidate.citationAccuracy(), false));
        result.put("groundedness", winner(baselineId, candidateId, baseline.groundedness(), candidate.groundedness(), false));
        result.put("coreClaimCitationCoverage", winner(baselineId, candidateId, baseline.coreClaimCitationCoverage(), candidate.coreClaimCitationCoverage(), false));
        result.put("uncitedContextLeak", winner(baselineId, candidateId, baseline.uncitedContextLeak() ? 1.0 : 0.0, candidate.uncitedContextLeak() ? 1.0 : 0.0, true));
        return result;
    }

    private String winner(String baselineId, String candidateId, double baseline, double candidate, boolean lowerIsBetter) {
        if (Double.compare(baseline, candidate) == 0) {
            return safeId(baselineId);
        }
        boolean candidateWins = lowerIsBetter ? candidate < baseline : candidate > baseline;
        return candidateWins ? safeId(candidateId) : safeId(baselineId);
    }

    private String safeId(String id) {
        return id == null || id.isBlank() ? "unknown" : id;
    }

    private double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }

    private record SampleEvaluation(RagEvaluationResult.SampleResult result) {
    }

    private static final class MetricsAccumulator {
        private int expectedSourceCount;
        private int evaluatedCitationCount;
        private int relevantCitationCount;
        private int noSourceSampleCount;
        private int noSourceRefusalCount;
        private int sourceRequiredSampleCount;
        private int leakCount;
        private double recallTotal;
        private double citationAccuracyTotal;
        private double coverageTotal;

        private void add(SampleEvaluation evaluation) {
            RagEvaluationResult.SampleResult sample = evaluation.result();
            if (sample.expectedNoSource()) {
                noSourceSampleCount++;
                if (sample.noSourceRefusal()) {
                    noSourceRefusalCount++;
                }
                return;
            }
            sourceRequiredSampleCount++;
            recallTotal += sample.recallAtK();
            citationAccuracyTotal += sample.citationAccuracy();
            coverageTotal += sample.coreClaimCitationCoverage();
            if (sample.uncitedContextLeak()) {
                leakCount++;
            }
            expectedSourceCount += sample.expectedChunkCount();
            evaluatedCitationCount += sample.evaluatedCitationCount();
            relevantCitationCount += sample.relevantCitationCount();
        }

        private RagEvaluationResult.StrategyMetrics toMetrics() {
            double recallAtK = sourceRequiredSampleCount == 0 ? 1.0 : recallTotal / sourceRequiredSampleCount;
            double citationAccuracy = sourceRequiredSampleCount == 0 ? 1.0 : citationAccuracyTotal / sourceRequiredSampleCount;
            double groundedness = harmonicMeanStatic(recallAtK, citationAccuracy);
            double noSourceRefusalRate = noSourceSampleCount == 0 ? 1.0 : (double) noSourceRefusalCount / noSourceSampleCount;
            double coverage = sourceRequiredSampleCount == 0 ? 1.0 : coverageTotal / sourceRequiredSampleCount;
            double leakRate = sourceRequiredSampleCount == 0 ? 0.0 : (double) leakCount / sourceRequiredSampleCount;
            return new RagEvaluationResult.StrategyMetrics(
                    recallAtK,
                    citationAccuracy,
                    groundedness,
                    noSourceRefusalRate,
                    coverage,
                    leakRate,
                    expectedSourceCount,
                    evaluatedCitationCount,
                    relevantCitationCount
            );
        }

        private static double harmonicMeanStatic(double first, double second) {
            if (first == 0.0 || second == 0.0) {
                return 0.0;
            }
            return 2.0 * first * second / (first + second);
        }
    }
}
