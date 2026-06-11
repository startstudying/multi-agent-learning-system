package com.learningos.rag.application;

public record VectorUpsertResult(
        VectorIndexStatus status,
        int upsertCount,
        long latencyMs,
        String errorCode
) {
    public static VectorUpsertResult disabled() {
        return new VectorUpsertResult(VectorIndexStatus.DISABLED, 0, 0L, null);
    }
}
