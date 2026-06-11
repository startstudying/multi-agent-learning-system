package com.learningos.rag.application;

import java.util.Arrays;

public record EmbeddingVector(
        String ownerId,
        float[] values
) {
    public EmbeddingVector {
        values = values == null ? new float[0] : Arrays.copyOf(values, values.length);
    }

    @Override
    public float[] values() {
        return Arrays.copyOf(values, values.length);
    }

    public int dimension() {
        return values.length;
    }

    @Override
    public String toString() {
        return "EmbeddingVector[ownerId=" + ownerId + ", dimension=" + dimension() + "]";
    }
}
