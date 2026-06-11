package com.learningos.rag.application;

public record VectorSearchHit(
        String chunkId,
        double score
) {
}
