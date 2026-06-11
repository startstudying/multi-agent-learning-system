package com.learningos.rag.vector;

import java.util.Arrays;

public record QdrantVectorPoint(
        String chunkId,
        String kbId,
        String documentId,
        int documentVersion,
        String chunkHash,
        Integer chunkIndex,
        float[] vector
) {
    public QdrantVectorPoint {
        vector = vector == null ? new float[0] : Arrays.copyOf(vector, vector.length);
    }

    @Override
    public float[] vector() {
        return Arrays.copyOf(vector, vector.length);
    }

    @Override
    public String toString() {
        return "QdrantVectorPoint[chunkId=" + chunkId
                + ", kbId=" + kbId
                + ", documentId=" + documentId
                + ", documentVersion=" + documentVersion
                + ", chunkHash=" + chunkHash
                + ", chunkIndex=" + chunkIndex
                + ", vectorDimension=" + vector.length
                + "]";
    }
}
