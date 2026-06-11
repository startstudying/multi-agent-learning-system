package com.learningos.rag.application;

public record VectorChunkReference(
        String chunkId,
        String chunkHash,
        Integer chunkIndex,
        EmbeddingVector vector
) {
    public VectorChunkReference(String chunkId, String chunkHash, Integer chunkIndex) {
        this(chunkId, chunkHash, chunkIndex, null);
    }

    @Override
    public String toString() {
        return "VectorChunkReference[chunkId=" + chunkId
                + ", chunkHash=" + chunkHash
                + ", chunkIndex=" + chunkIndex
                + ", vectorDimension=" + (vector == null ? 0 : vector.dimension())
                + "]";
    }
}
