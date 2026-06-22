package com.learningos.aiqa.application.quality;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.api.dto.AiQaDtos.NextStep;
import com.learningos.aiqa.api.dto.AiQaDtos.ToolCallSummary;
import com.learningos.aiqa.api.dto.AiQaDtos.VerificationCheck;
import com.learningos.aiqa.api.dto.AiQaDtos.VerificationSummary;
import com.learningos.rag.application.CitationCoverageAnalyzer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AnswerVerifier {

    private static final String POLICY = "BASIC_QA_VERIFIER_V1";
    private final CitationCoverageAnalyzer citationCoverageAnalyzer = new CitationCoverageAnalyzer();
    private static final List<String> SENSITIVE_MARKERS = List.of(
            "chain-of-thought",
            "raw chain",
            "raw prompt",
            "rawprompt",
            "prompt:",
            "provider key",
            "provider_key",
            "api key",
            "api_key",
            "sk-",
            "teacher_note",
            "teacher note",
            "profilesnapshot"
    );

    public VerificationSummary verify(AiQaResponse response) {
        List<VerificationCheck> checks = List.of(
                schemaCheck(response),
                citationConsistencyCheck(response),
                groundedCitationCheck(response),
                coreClaimCitationCoverageCheck(response),
                uncitedContextLeakCheck(response),
                noSourcePolicyCheck(response),
                privacyCheck(response)
        );
        boolean failed = checks.stream().anyMatch(check -> "FAIL".equals(check.status()));
        boolean warning = checks.stream().anyMatch(check -> "WARN".equals(check.status()));
        boolean noSource = isNoSource(response);
        List<String> qualityFlags = new ArrayList<>();
        qualityFlags.add(failed ? "ANSWER_VERIFICATION_FAILED" : "ANSWER_VERIFIED");
        if (warning) {
            qualityFlags.add("ANSWER_REVIEW_RECOMMENDED");
        }
        if (checks.stream().anyMatch(check -> "CORE_CLAIM_CITATION_COVERAGE".equals(check.name()) && "WARN".equals(check.status()))) {
            qualityFlags.add("CORE_CLAIM_CITATION_REVIEW");
        }
        if (checks.stream().anyMatch(check -> "UNCITED_CONTEXT_LEAK_GUARD".equals(check.name()) && "WARN".equals(check.status()))) {
            qualityFlags.add("UNCITED_CONTEXT_LEAK_REVIEW");
        }
        if (!failed && noSource) {
            qualityFlags.add("NO_SOURCE_VERIFIED");
        }
        return new VerificationSummary(
                failed ? "FAIL" : "PASS",
                checks,
                List.copyOf(qualityFlags),
                failed || warning || (response != null && response.requiresReview()),
                POLICY
        );
    }

    public ToolCallSummary toolCallSummary(VerificationSummary verification) {
        long failedChecks = verification.checks().stream()
                .filter(check -> "FAIL".equals(check.status()))
                .count();
        return new ToolCallSummary(
                "AnswerVerifier",
                failedChecks == 0 ? "SUCCESS" : "FAILED",
                "policy=" + POLICY
                        + ";verdict=" + verification.verdict()
                        + ";checks=" + verification.checks().size()
                        + ";failedChecks=" + failedChecks
                        + ";requiresReview=" + verification.requiresReview()
        );
    }

    private VerificationCheck schemaCheck(AiQaResponse response) {
        boolean pass = response != null
                && hasText(response.answer())
                && hasText(response.sourceStatus())
                && hasText(response.sourcePolicy())
                && hasText(response.traceId())
                && response.learnerFit() != null
                && response.nextSteps() != null
                && !response.nextSteps().isEmpty()
                && response.uncertainty() != null
                && response.qualityFlags() != null
                && response.qualityFlags().contains("STRUCTURED_SCHEMA_V1");
        return check(
                "SCHEMA_REQUIRED_FIELDS",
                pass,
                "BLOCKER",
                pass ? "Required response schema fields are present." : "Required response schema fields are missing."
        );
    }

    private VerificationCheck citationConsistencyCheck(AiQaResponse response) {
        List<?> sources = response == null || response.sources() == null ? List.of() : response.sources();
        List<?> citations = response == null || response.citations() == null ? List.of() : response.citations();
        boolean pass = sources.equals(citations);
        return check(
                "CITATION_CONSISTENCY",
                pass,
                "BLOCKER",
                pass ? "sources and citations are consistent." : "sources and citations diverge."
        );
    }

    private VerificationCheck groundedCitationCheck(AiQaResponse response) {
        boolean grounded = response != null && "COURSE_GROUNDED".equals(response.sourceStatus());
        boolean pass = !grounded || (response.citations() != null && !response.citations().isEmpty());
        return check(
                "GROUNDED_CITATION_REQUIRED",
                pass,
                "BLOCKER",
                pass ? "Grounded answer citation policy is satisfied." : "Grounded answer has no citation."
        );
    }

    private VerificationCheck coreClaimCitationCoverageCheck(AiQaResponse response) {
        if (!isGroundedWithCitations(response)) {
            return check("CORE_CLAIM_CITATION_COVERAGE", true, "WARN", "Core claim coverage check is not applicable.");
        }
        CitationCoverageAnalyzer.CoverageResult coverage = citationCoverageAnalyzer.analyze(response.answer(), response.citations());
        boolean pass = coverage.coreClaimCount() <= 1 || coverage.coveredCoreClaimCount() == coverage.coreClaimCount();
        return check(
                "CORE_CLAIM_CITATION_COVERAGE",
                pass,
                "WARN",
                pass
                        ? "Core answer claims are covered by visible citations."
                        : "One or more core answer claims are not covered by visible citations."
        );
    }

    private VerificationCheck uncitedContextLeakCheck(AiQaResponse response) {
        if (!isGroundedWithCitations(response)) {
            return check("UNCITED_CONTEXT_LEAK_GUARD", true, "WARN", "Uncited context leak check is not applicable.");
        }
        CitationCoverageAnalyzer.CoverageResult coverage = citationCoverageAnalyzer.analyze(response.answer(), response.citations());
        return check(
                "UNCITED_CONTEXT_LEAK_GUARD",
                !coverage.uncitedContextLeak(),
                "WARN",
                coverage.uncitedContextLeak()
                        ? "Answer may include context not covered by visible citations."
                        : "No uncited context mixing was detected."
        );
    }

    private VerificationCheck noSourcePolicyCheck(AiQaResponse response) {
        if (!isNoSource(response)) {
            return check("NO_SOURCE_POLICY", true, "BLOCKER", "No-source policy is not applicable.");
        }
        boolean emptyCitations = isEmpty(response.sources()) && isEmpty(response.citations());
        boolean hasFlag = response.qualityFlags() != null && response.qualityFlags().contains("NO_SOURCE_FALLBACK");
        boolean uncertainty = response.uncertainty() != null
                && ("MEDIUM".equals(response.uncertainty().level()) || "HIGH".equals(response.uncertainty().level()));
        boolean pass = emptyCitations && hasFlag && uncertainty && response.requiresReview();
        return check(
                "NO_SOURCE_POLICY",
                pass,
                "BLOCKER",
                pass ? "No-source fallback policy is satisfied." : "No-source fallback policy is incomplete."
        );
    }

    private VerificationCheck privacyCheck(AiQaResponse response) {
        boolean pass = response != null && !containsSensitiveMarker(visibleText(response));
        return check(
                "PRIVACY_LEAK_GUARD",
                pass,
                "BLOCKER",
                pass ? "Visible response summaries are privacy-safe." : "Visible response summaries contain sensitive markers."
        );
    }

    private boolean isNoSource(AiQaResponse response) {
        return response != null
                && ("GENERAL_FALLBACK".equals(response.sourceStatus())
                || "NO_COURSE_SOURCE_FALLBACK".equals(response.sourcePolicy()));
    }

    private boolean isGroundedWithCitations(AiQaResponse response) {
        return response != null
                && "COURSE_GROUNDED".equals(response.sourceStatus())
                && response.citations() != null
                && !response.citations().isEmpty();
    }

    private String visibleText(AiQaResponse response) {
        StringBuilder builder = new StringBuilder();
        append(builder, response.answer());
        append(builder, response.reasoningSummary());
        if (response.learnerFit() != null) {
            append(builder, response.learnerFit().summary());
        }
        if (response.nextSteps() != null) {
            for (NextStep step : response.nextSteps()) {
                append(builder, step.title());
                append(builder, step.action());
            }
        }
        if (response.uncertainty() != null) {
            append(builder, response.uncertainty().reason());
            if (response.uncertainty().factors() != null) {
                response.uncertainty().factors().forEach(factor -> append(builder, factor));
            }
        }
        if (response.toolCalls() != null) {
            for (ToolCallSummary toolCall : response.toolCalls()) {
                append(builder, toolCall.name());
                append(builder, toolCall.status());
                append(builder, toolCall.summary());
            }
        }
        return builder.toString();
    }

    private boolean containsSensitiveMarker(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return SENSITIVE_MARKERS.stream().anyMatch(normalized::contains);
    }

    private VerificationCheck check(String name, boolean pass, String severity, String message) {
        String status = pass ? "PASS" : "WARN".equals(severity) ? "WARN" : "FAIL";
        return new VerificationCheck(name, status, severity, message);
    }

    private void append(StringBuilder builder, String value) {
        if (value != null) {
            builder.append(' ').append(value);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }
}
