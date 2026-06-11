package com.learningos.rag.application;

public record QueryEmbeddingResult(
        EmbeddingStatus status,
        String modelVersion,
        EmbeddingVector vector,
        long latencyMs,
        String errorCode
) {
    public static QueryEmbeddingResult disabled(String modelVersion) {
        return new QueryEmbeddingResult(EmbeddingStatus.DISABLED, modelVersion, null, 0L, null);
    }
}
