package com.learningos.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LearningOsMetrics {

    private static final int MAX_TAG_VALUE_LENGTH = 120;

    private final MeterRegistry meterRegistry;

    public LearningOsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private LearningOsMetrics() {
        this.meterRegistry = null;
    }

    public static LearningOsMetrics noop() {
        return new LearningOsMetrics();
    }

    public void recordHttpRequest(String method, String route, int status, String errorCode, long latencyMs) {
        if (!enabled()) {
            return;
        }
        String statusValue = Integer.toString(status);
        Timer.builder("learningos.http.server.requests")
                .tags(
                        "method", tag(method),
                        "route", tag(route),
                        "status", statusValue,
                        "error_code", tag(errorCode)
                )
                .register(meterRegistry)
                .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
        if (status >= 400) {
            Counter.builder("learningos.http.server.failures")
                    .tags(
                            "method", tag(method),
                            "route", tag(route),
                            "status", statusValue,
                            "error_code", tag(errorCode)
                    )
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordRagQuery(
            String strategy,
            String outcome,
            boolean noSource,
            boolean replayed,
            String errorCode,
            Long latencyMs,
            Integer retrievalCount,
            Integer citationCount
    ) {
        if (!enabled()) {
            return;
        }
        String normalizedStrategy = tag(strategy);
        String normalizedOutcome = tag(outcome);
        String noSourceTag = Boolean.toString(noSource);
        String replayedTag = Boolean.toString(replayed);
        String normalizedErrorCode = tag(errorCode);

        Counter.builder("learningos.rag.query.count")
                .tags(
                        "strategy", normalizedStrategy,
                        "outcome", normalizedOutcome,
                        "no_source", noSourceTag,
                        "replayed", replayedTag,
                        "error_code", normalizedErrorCode
                )
                .register(meterRegistry)
                .increment();

        if (latencyMs != null) {
            Timer.builder("learningos.rag.query.duration")
                    .tags(
                            "strategy", normalizedStrategy,
                            "outcome", normalizedOutcome,
                            "no_source", noSourceTag,
                            "replayed", replayedTag,
                            "error_code", normalizedErrorCode
                    )
                    .register(meterRegistry)
                    .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
        }
        if (retrievalCount != null) {
            DistributionSummary.builder("learningos.rag.retrieval.count")
                    .tags("strategy", normalizedStrategy, "no_source", noSourceTag)
                    .register(meterRegistry)
                    .record(Math.max(0, retrievalCount));
        }
        if (citationCount != null) {
            DistributionSummary.builder("learningos.rag.citation.count")
                    .tags("strategy", normalizedStrategy, "no_source", noSourceTag)
                    .register(meterRegistry)
                    .record(Math.max(0, citationCount));
        }
    }

    public void recordRagFailure(String strategy, String errorCode) {
        if (!enabled()) {
            return;
        }
        String normalizedStrategy = tag(strategy);
        String normalizedErrorCode = tag(errorCode);
        Counter.builder("learningos.rag.query.failures")
                .tags(
                        "strategy", normalizedStrategy,
                        "outcome", "error",
                        "no_source", "false",
                        "replayed", "false",
                        "error_code", normalizedErrorCode
                )
                .register(meterRegistry)
                .increment();
        recordRagQuery(normalizedStrategy, "error", false, false, normalizedErrorCode, null, null, null);
    }

    public void recordModelCall(
            String agentName,
            String provider,
            String model,
            String status,
            String errorCode,
            long latencyMs
    ) {
        if (!enabled()) {
            return;
        }
        String normalizedAgent = tag(agentName);
        String normalizedProvider = tag(provider);
        String normalizedModel = tag(model);
        String normalizedStatus = tag(status);
        Timer.builder("learningos.model.call.duration")
                .tags(
                        "agent_name", normalizedAgent,
                        "provider", normalizedProvider,
                        "model", normalizedModel,
                        "status", normalizedStatus
                )
                .register(meterRegistry)
                .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
        if (!"SUCCESS".equalsIgnoreCase(normalizedStatus)) {
            Counter.builder("learningos.model.call.failures")
                    .tags(
                            "agent_name", normalizedAgent,
                            "provider", normalizedProvider,
                            "model", normalizedModel,
                            "error_code", tag(errorCode)
                    )
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordTokenUsage(String model, String promptCode, Integer promptTokens, Integer completionTokens, Integer totalTokens, Double estimatedCost) {
        if (!enabled()) {
            return;
        }
        recordTokenAmount(model, promptCode, "prompt", promptTokens);
        recordTokenAmount(model, promptCode, "completion", completionTokens);
        recordTokenAmount(model, promptCode, "total", totalTokens);
        DistributionSummary.builder("learningos.token.cost")
                .tags(
                        "model", tag(model),
                        "prompt_code", tag(promptCode),
                        "currency", "USD"
                )
                .register(meterRegistry)
                .record(Math.max(0.0, estimatedCost == null ? 0.0 : estimatedCost));
    }

    private void recordTokenAmount(String model, String promptCode, String tokenType, Integer amount) {
        DistributionSummary.builder("learningos.token.usage")
                .tags(
                        "model", tag(model),
                        "prompt_code", tag(promptCode),
                        "token_type", tokenType
                )
                .register(meterRegistry)
                .record(Math.max(0, amount == null ? 0 : amount));
    }

    private boolean enabled() {
        return meterRegistry != null;
    }

    private String tag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isISOControl(character)) {
                builder.append(character);
            }
        }
        String sanitized = builder.toString().trim();
        if (sanitized.isBlank()) {
            return "unknown";
        }
        if (sanitized.length() > MAX_TAG_VALUE_LENGTH) {
            return sanitized.substring(0, MAX_TAG_VALUE_LENGTH);
        }
        return sanitized;
    }
}
