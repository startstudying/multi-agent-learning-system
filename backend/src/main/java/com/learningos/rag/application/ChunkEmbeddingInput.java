package com.learningos.rag.application;

public record ChunkEmbeddingInput(
        String chunkId,
        String content
) {
}
