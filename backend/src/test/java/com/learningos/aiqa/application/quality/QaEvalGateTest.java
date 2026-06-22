package com.learningos.aiqa.application.quality;

import com.learningos.aiqa.application.quality.QaEvalGate.MetricThreshold;
import com.learningos.aiqa.application.quality.QaEvalGate.QaEvalGateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QaEvalGateTest {

    private final QaEvalGate gate = new QaEvalGate();

    @Test
    void returnsInsufficientSampleWhenRunIsTooSmall() {
        var result = gate.evaluate(new QaEvalGateRequest(
                2,
                5,
                false,
                null,
                null,
                defaultThresholds(),
                passingMetrics()
        ));

        assertThat(result.verdict()).isEqualTo("INSUFFICIENT_SAMPLE");
        assertThat(result.reason()).contains("sample");
    }

    @Test
    void failsWhenRequiredMetricsAreMissing() {
        var result = gate.evaluate(new QaEvalGateRequest(
                10,
                5,
                false,
                null,
                null,
                defaultThresholds(),
                Map.of("groundedness", 0.90)
        ));

        assertThat(result.verdict()).isEqualTo("FAIL");
        assertThat(result.missingMetrics()).contains("citationAccuracy", "noSourceRefusalRate", "schemaPassRate", "verificationPassRate", "privacyLeakRate");
    }

    @Test
    void failsWhenStrategyChangedWithoutBaselineAndCandidate() {
        var result = gate.evaluate(new QaEvalGateRequest(
                10,
                5,
                true,
                "qa-runtime-v1",
                "",
                defaultThresholds(),
                passingMetrics()
        ));

        assertThat(result.verdict()).isEqualTo("FAIL");
        assertThat(result.reason()).contains("baseline", "candidate");
    }

    @Test
    void passesWhenSampleMetricsAndComparisonContextMeetGate() {
        var result = gate.evaluate(new QaEvalGateRequest(
                12,
                5,
                true,
                "qa-runtime-v1",
                "qa-runtime-v2",
                defaultThresholds(),
                passingMetrics()
        ));

        assertThat(result.verdict()).isEqualTo("PASS");
        assertThat(result.missingMetrics()).isEmpty();
        assertThat(result.checks()).allSatisfy(check -> assertThat(check.status()).isEqualTo("PASS"));
    }

    @Test
    void acceptsCitationCoverageAndUncitedContextLeakThresholds() {
        List<MetricThreshold> thresholds = List.of(
                new MetricThreshold("coreClaimCitationCoverage", 0.85, "AT_LEAST"),
                new MetricThreshold("uncitedContextLeakRate", 0.05, "AT_MOST")
        );

        var result = gate.evaluate(new QaEvalGateRequest(
                12,
                5,
                true,
                "baseline-topk",
                "poc-context",
                thresholds,
                Map.of(
                        "coreClaimCitationCoverage", 0.92,
                        "uncitedContextLeakRate", 0.02
                )
        ));

        assertThat(result.verdict()).isEqualTo("PASS");
        assertThat(result.checks()).allSatisfy(check -> assertThat(check.status()).isEqualTo("PASS"));
    }

    private List<MetricThreshold> defaultThresholds() {
        return List.of(
                new MetricThreshold("groundedness", 0.80, "AT_LEAST"),
                new MetricThreshold("citationAccuracy", 0.80, "AT_LEAST"),
                new MetricThreshold("noSourceRefusalRate", 0.90, "AT_LEAST"),
                new MetricThreshold("schemaPassRate", 0.95, "AT_LEAST"),
                new MetricThreshold("verificationPassRate", 0.95, "AT_LEAST"),
                new MetricThreshold("privacyLeakRate", 0.00, "AT_MOST")
        );
    }

    private Map<String, Double> passingMetrics() {
        return Map.of(
                "groundedness", 0.91,
                "citationAccuracy", 0.92,
                "noSourceRefusalRate", 1.0,
                "schemaPassRate", 0.98,
                "verificationPassRate", 0.97,
                "privacyLeakRate", 0.0
        );
    }
}
