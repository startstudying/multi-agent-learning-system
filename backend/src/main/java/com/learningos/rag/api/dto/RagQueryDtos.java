package com.learningos.rag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class RagQueryDtos {

    private RagQueryDtos() {
    }

    public record RagQueryRequest(
            @NotEmpty List<String> kbIds,
            @NotBlank String question,
            Integer topK,
            String requestId
    ) {
    }

    public record RagQueryResponse(
            String answer,
            List<SourceCitation> sources,
            String traceId,
            RetrievalMetadata retrieval
    ) {
        public RagQueryResponse(String answer, List<SourceCitation> sources, String traceId) {
            this(answer, sources, traceId, RetrievalMetadata.legacyFallback(sources == null ? 0 : sources.size()));
        }
    }

    public record SourceCitation(
            String documentId,
            String documentName,
            Integer pageNum,
            String sectionTitle,
            String excerpt,
            double score
    ) {
    }

    public record RetrievalMetadata(
            String strategy,
            String queryComplexity,
            boolean noSource,
            int retrievalCount,
            int candidateCount,
            int citationCount,
            boolean downgraded,
            String message
    ) {
        public RetrievalMetadata(String strategy, String queryComplexity, boolean noSource, int retrievalCount, String message) {
            this(strategy, queryComplexity, noSource, retrievalCount, retrievalCount, retrievalCount, noSource, message);
        }

        public static RetrievalMetadata legacyFallback(int retrievalCount) {
            return new RetrievalMetadata(
                    retrievalCount == 0 ? "NO_SOURCE_REFUSAL" : "COURSE_RAG",
                    "UNKNOWN",
                    retrievalCount == 0,
                    retrievalCount,
                    retrievalCount,
                    retrievalCount,
                    retrievalCount == 0,
                    retrievalCount == 0
                            ? "No cited course material was available for this response."
                            : "Response was grounded with cited course material."
            );
        }
    }
}
