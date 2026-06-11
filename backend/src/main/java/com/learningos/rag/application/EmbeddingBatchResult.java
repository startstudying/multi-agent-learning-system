package com.learningos.rag.application;

import java.util.List;

public record EmbeddingBatchResult(
        EmbeddingStatus status,
        String modelVersion,
        int chunkCount,
        long latencyMs,
        String errorCode,
        List<EmbeddingVector> vectors
) {
    public EmbeddingBatchResult {
        vectors = vectors == null ? List.of() : List.copyOf(vectors);
    }

    public EmbeddingBatchResult(
            EmbeddingStatus status,
            String modelVersion,
            int chunkCount,
            long latencyMs,
            String errorCode
    ) {
        this(status, modelVersion, chunkCount, latencyMs, errorCode, List.of());
    }

    public static EmbeddingBatchResult disabled(String modelVersion, int chunkCount) {
        return new EmbeddingBatchResult(EmbeddingStatus.DISABLED, modelVersion, chunkCount, 0L, null, List.of());
    }
}
