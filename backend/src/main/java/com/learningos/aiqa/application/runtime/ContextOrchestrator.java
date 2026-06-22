package com.learningos.aiqa.application.runtime;

import com.learningos.aiqa.application.memory.MemoryContext;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import org.springframework.stereotype.Component;

@Component
public class ContextOrchestrator {

    public QaContextEnvelope build(
            IntentRouter.QaIntent intent,
            MemoryContext memoryContext,
            RagQueryResponse ragResponse,
            boolean generalFallback
    ) {
        int contextItems = memoryContext == null ? 0 : memoryContext.contextItemCount();
        int estimatedTokens = memoryContext == null || memoryContext.budget() == null
                ? 0
                : memoryContext.budget().estimatedTokens();
        boolean memoryTruncated = memoryContext != null
                && memoryContext.budget() != null
                && memoryContext.budget().truncated();
        int citationCount = ragResponse == null || ragResponse.sources() == null ? 0 : ragResponse.sources().size();
        String contextSummary = "intent=" + (intent == null ? "UNKNOWN" : intent.taskType())
                + ";policy=" + (intent == null ? "UNKNOWN" : intent.qualityPolicy())
                + ";contextItems=" + contextItems
                + ";citationCount=" + citationCount
                + ";sourceState=" + (generalFallback ? "NO_SOURCE" : "COURSE_GROUNDED")
                + ";memoryTruncated=" + memoryTruncated;
        return new QaContextEnvelope(
                contextSummary,
                contextItems,
                citationCount,
                estimatedTokens,
                memoryTruncated
        );
    }

    public record QaContextEnvelope(
            String contextSummary,
            int contextItems,
            int citationCount,
            int estimatedTokens,
            boolean memoryTruncated
    ) {
        public String summary() {
            return contextSummary + ";estimatedTokens=" + estimatedTokens;
        }
    }
}
