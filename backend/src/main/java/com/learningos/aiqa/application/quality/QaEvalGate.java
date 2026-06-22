package com.learningos.aiqa.application.quality;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QaEvalGate {

    public QaEvalGateResult evaluate(QaEvalGateRequest request) {
        QaEvalGateRequest safeRequest = request == null
                ? new QaEvalGateRequest(0, 5, false, null, null, List.of(), Map.of())
                : request;
        int minimumSampleCount = safeRequest.minimumSampleCount() <= 0 ? 5 : safeRequest.minimumSampleCount();
        if (safeRequest.sampleCount() < minimumSampleCount) {
            return new QaEvalGateResult(
                    "INSUFFICIENT_SAMPLE",
                    safeRequest.sampleCount(),
                    minimumSampleCount,
                    List.of(),
                    thresholdNames(safeRequest.thresholds()),
                    "Evaluation sample count is below the minimum sample threshold."
            );
        }

        List<String> missingMetrics = missingMetrics(safeRequest);
        if (!missingMetrics.isEmpty()) {
            return new QaEvalGateResult(
                    "FAIL",
                    safeRequest.sampleCount(),
                    minimumSampleCount,
                    List.of(),
                    missingMetrics,
                    "Missing required QA gate metrics."
            );
        }

        List<MetricCheck> checks = metricChecks(safeRequest);
        boolean metricFailed = checks.stream().anyMatch(check -> "FAIL".equals(check.status()));
        if (metricFailed) {
            return new QaEvalGateResult(
                    "FAIL",
                    safeRequest.sampleCount(),
                    minimumSampleCount,
                    checks,
                    List.of(),
                    "One or more QA gate metrics are outside threshold."
            );
        }

        if (safeRequest.strategyChanged() && (!hasText(safeRequest.baselineId()) || !hasText(safeRequest.candidateId()))) {
            return new QaEvalGateResult(
                    "FAIL",
                    safeRequest.sampleCount(),
                    minimumSampleCount,
                    checks,
                    List.of(),
                    "Strategy changes require both baseline and candidate identifiers."
            );
        }

        return new QaEvalGateResult(
                "PASS",
                safeRequest.sampleCount(),
                minimumSampleCount,
                checks,
                List.of(),
                "QA eval gate passed."
        );
    }

    private List<String> missingMetrics(QaEvalGateRequest request) {
        Map<String, Double> metrics = request.metrics() == null ? Map.of() : request.metrics();
        return request.thresholds().stream()
                .map(MetricThreshold::metricName)
                .filter(metricName -> !metrics.containsKey(metricName))
                .toList();
    }

    private List<String> thresholdNames(List<MetricThreshold> thresholds) {
        if (thresholds == null) {
            return List.of();
        }
        return thresholds.stream().map(MetricThreshold::metricName).toList();
    }

    private List<MetricCheck> metricChecks(QaEvalGateRequest request) {
        List<MetricCheck> checks = new ArrayList<>();
        Map<String, Double> metrics = request.metrics();
        for (MetricThreshold threshold : request.thresholds()) {
            double actual = metrics.get(threshold.metricName());
            boolean pass = switch (threshold.direction()) {
                case "AT_MOST" -> actual <= threshold.threshold();
                default -> actual >= threshold.threshold();
            };
            checks.add(new MetricCheck(
                    threshold.metricName(),
                    pass ? "PASS" : "FAIL",
                    actual,
                    threshold.threshold(),
                    threshold.direction()
            ));
        }
        return List.copyOf(checks);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record QaEvalGateRequest(
            int sampleCount,
            int minimumSampleCount,
            boolean strategyChanged,
            String baselineId,
            String candidateId,
            List<MetricThreshold> thresholds,
            Map<String, Double> metrics
    ) {
        public QaEvalGateRequest {
            thresholds = thresholds == null ? List.of() : List.copyOf(thresholds);
            metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        }
    }

    public record MetricThreshold(
            String metricName,
            double threshold,
            String direction
    ) {
        public MetricThreshold {
            direction = "AT_MOST".equals(direction) ? "AT_MOST" : "AT_LEAST";
        }
    }

    public record QaEvalGateResult(
            String verdict,
            int sampleCount,
            int minimumSampleCount,
            List<MetricCheck> checks,
            List<String> missingMetrics,
            String reason
    ) {
    }

    public record MetricCheck(
            String metricName,
            String status,
            double actual,
            double threshold,
            String direction
    ) {
    }
}
