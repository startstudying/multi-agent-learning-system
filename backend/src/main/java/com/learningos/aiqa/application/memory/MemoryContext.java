package com.learningos.aiqa.application.memory;

import java.util.List;

public record MemoryContext(
        LearnerSummary learner,
        List<LearningSignal> learningSignals,
        List<RagCitationContext> citations,
        List<RecentSessionSummary> recentSessions,
        List<PreferenceMemory> preferences,
        List<ContextInjectionReason> injectionReasons,
        TokenBudget budget
) {

    public String summary() {
        return "contextItems=" + contextItemCount()
                + ";learner=" + (learner == null ? "none" : learner.profileRef())
                + ";learningSignals=" + sizeOf(learningSignals)
                + ";citations=" + sizeOf(citations)
                + ";recentSessions=" + sizeOf(recentSessions)
                + ";preferences=" + sizeOf(preferences)
                + ";budget=" + (budget == null ? "0/0" : budget.estimatedTokens() + "/" + budget.maxTokens())
                + ";truncated=" + (budget != null && budget.truncated());
    }

    public int contextItemCount() {
        return (learner == null ? 0 : 1)
                + sizeOf(learningSignals)
                + sizeOf(citations)
                + sizeOf(recentSessions)
                + sizeOf(preferences);
    }

    private int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    public record LearnerSummary(
            String profileRef,
            String target,
            List<String> weakPoints,
            String source,
            double score,
            String reason
    ) {
    }

    public record LearningSignal(
            String type,
            String referenceId,
            String summary,
            String source,
            double score,
            String reason
    ) {
    }

    public record RagCitationContext(
            String documentId,
            String documentName,
            Integer pageNum,
            String sectionTitle,
            String excerptSummary,
            String source,
            double score,
            String reason
    ) {
    }

    public record RecentSessionSummary(
            String referenceId,
            String summary,
            String source,
            double score,
            String reason
    ) {
    }

    public record PreferenceMemory(
            String key,
            String value,
            String source,
            double score,
            String reason
    ) {
    }

    public record ContextInjectionReason(
            String contextType,
            String source,
            double score,
            String reason
    ) {
    }

    public record TokenBudget(
            int maxTokens,
            int estimatedTokens,
            int remainingTokens,
            boolean truncated
    ) {
    }
}
