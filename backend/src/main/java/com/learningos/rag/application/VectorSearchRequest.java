package com.learningos.rag.application;

import java.util.List;

public record VectorSearchRequest(
        List<String> allowedKbIds,
        int topK,
        EmbeddingVector queryVector
) {
    @Override
    public String toString() {
        return "VectorSearchRequest[allowedKbIds=" + allowedKbIds
                + ", topK=" + topK
                + ", queryVectorDimension=" + (queryVector == null ? 0 : queryVector.dimension())
                + "]";
    }
}
