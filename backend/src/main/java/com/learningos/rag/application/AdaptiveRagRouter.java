package com.learningos.rag.application;

import com.learningos.rag.api.dto.RagQueryDtos.RetrievalMetadata;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AdaptiveRagRouter {

    public RetrievalMetadata describe(String question, int candidateCount, int retrievalCount, int citationCount) {
        return describe(question, candidateCount, retrievalCount, citationCount, false, null);
    }

    public RetrievalMetadata describe(
            String question,
            int candidateCount,
            int retrievalCount,
            int citationCount,
            boolean rerankerFallbackUsed,
            String rerankerStatus
    ) {
        QueryComplexity complexity = classify(question);
        RetrievalStrategy strategy = chooseStrategy(question, complexity, candidateCount, citationCount);
        boolean noSource = strategy == RetrievalStrategy.NO_SOURCE_REFUSAL;
        boolean downgraded = noSource || rerankerFallbackUsed || retrievalCount < candidateCount;

        if (noSource) {
            return new RetrievalMetadata(
                    strategy.name(),
                    complexity.name(),
                    true,
                    retrievalCount,
                    candidateCount,
                    citationCount,
                    true,
                    "No indexed course chunks were available after permission filtering, so no citations were attached."
            );
        }

        return new RetrievalMetadata(
                strategy.name(),
                complexity.name(),
                false,
                retrievalCount,
                candidateCount,
                citationCount,
                downgraded,
                "Retrieved " + retrievalCount + " cited course chunk" + (retrievalCount == 1 ? "" : "s")
                        + " using the " + strategy.name() + " routing strategy."
                        + (rerankerFallbackUsed ? " Reranker fallback: " + rerankerStatus + "." : "")
        );
    }

    public RetrievalMetadata describe(String question, int retrievalCount) {
        return describe(question, retrievalCount, retrievalCount, retrievalCount);
    }

    private RetrievalStrategy chooseStrategy(
            String question,
            QueryComplexity complexity,
            int candidateCount,
            int citationCount
    ) {
        if (candidateCount <= 0 || citationCount <= 0) {
            return RetrievalStrategy.NO_SOURCE_REFUSAL;
        }

        String normalized = normalize(question);
        if (hasHistoryClue(normalized)) {
            return RetrievalStrategy.RAG_WITH_HISTORY;
        }
        if (hasProfileClue(normalized) || complexity == QueryComplexity.DIAGNOSTIC) {
            return RetrievalStrategy.RAG_WITH_PROFILE;
        }
        if (isDirectAnswerCandidate(normalized, complexity)) {
            return RetrievalStrategy.DIRECT_ANSWER;
        }
        return RetrievalStrategy.COURSE_RAG;
    }

    private QueryComplexity classify(String question) {
        String normalized = normalize(question);
        if (normalized.isBlank()) {
            return QueryComplexity.SIMPLE;
        }
        if (hasDiagnosticClue(normalized)) {
            return QueryComplexity.DIAGNOSTIC;
        }
        if (normalized.length() > 120
                || normalized.contains("why ")
                || normalized.contains("how ")
                || normalized.contains("\u4e3a\u4ec0\u4e48")
                || normalized.contains("\u5982\u4f55")
                || normalized.contains("compare")
                || normalized.contains("analyze")
                || normalized.contains("explain")
                || normalized.contains("\u6bd4\u8f83")
                || normalized.contains("\u5206\u6790")
                || normalized.contains("\u89e3\u91ca")) {
            return QueryComplexity.COMPLEX;
        }
        return QueryComplexity.SIMPLE;
    }

    private boolean hasDiagnosticClue(String normalized) {
        return containsAny(
                normalized,
                "diagnose",
                "diagnosis",
                "weak point",
                "weak points",
                "mistake",
                "mistakes",
                "missing",
                "wrong",
                "error pattern",
                "\u9519\u56e0",
                "\u8584\u5f31",
                "\u8bca\u65ad",
                "\u4e0d\u4f1a",
                "\u603b\u662f\u9519",
                "\u9519\u8bef"
        );
    }

    private boolean hasProfileClue(String normalized) {
        return containsAny(
                normalized,
                "profile",
                "learner profile",
                "my level",
                "my weakness",
                "my weak",
                "personalized",
                "\u753b\u50cf",
                "\u5b66\u4e60\u753b\u50cf",
                "\u6211\u7684\u6c34\u5e73",
                "\u6211\u7684\u8584\u5f31",
                "\u4e2a\u6027\u5316"
        );
    }

    private boolean hasHistoryClue(String normalized) {
        return containsAny(
                normalized,
                "history",
                "recent answer",
                "past answer",
                "previous answer",
                "wrong question",
                "answer record",
                "\u5386\u53f2",
                "\u6700\u8fd1\u7b54\u9898",
                "\u5386\u53f2\u7b54\u9898",
                "\u9519\u9898",
                "\u7b54\u9898\u8bb0\u5f55"
        );
    }

    private boolean isDirectAnswerCandidate(String normalized, QueryComplexity complexity) {
        return complexity == QueryComplexity.SIMPLE
                && containsAny(
                normalized,
                "platform",
                "how to use",
                "\u529f\u80fd",
                "\u5e73\u53f0",
                "\u600e\u4e48\u4f7f\u7528"
        );
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String question) {
        return question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
    }

    public enum RetrievalStrategy {
        DIRECT_ANSWER,
        COURSE_RAG,
        RAG_WITH_PROFILE,
        RAG_WITH_HISTORY,
        NO_SOURCE_REFUSAL
    }

    public enum QueryComplexity {
        SIMPLE,
        COMPLEX,
        DIAGNOSTIC
    }
}
