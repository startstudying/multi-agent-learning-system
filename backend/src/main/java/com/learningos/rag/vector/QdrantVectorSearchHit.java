package com.learningos.rag.vector;

public record QdrantVectorSearchHit(
        String chunkId,
        double score
) {
}
