package com.learningos.rag.application;

import java.util.List;

public record VectorSearchResult(
        VectorIndexStatus status,
        List<VectorSearchHit> hits,
        long latencyMs,
        String errorCode
) {
    public static VectorSearchResult disabled() {
        return new VectorSearchResult(VectorIndexStatus.DISABLED, List.of(), 0L, null);
    }

    public static VectorSearchResult succeeded(List<VectorSearchHit> hits) {
        return new VectorSearchResult(VectorIndexStatus.SUCCEEDED, hits == null ? List.of() : hits, 0L, null);
    }
}
