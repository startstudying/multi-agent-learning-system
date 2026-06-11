package com.learningos.rag.vector;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public record QdrantVectorSearchCommand(
        String collectionName,
        List<String> allowedKbIds,
        int topK,
        float[] queryVector,
        Duration timeout
) {
    public QdrantVectorSearchCommand {
        allowedKbIds = allowedKbIds == null ? List.of() : List.copyOf(allowedKbIds);
        queryVector = queryVector == null ? new float[0] : Arrays.copyOf(queryVector, queryVector.length);
    }

    @Override
    public float[] queryVector() {
        return Arrays.copyOf(queryVector, queryVector.length);
    }

    @Override
    public String toString() {
        return "QdrantVectorSearchCommand[collectionName=" + collectionName
                + ", allowedKbIds=" + allowedKbIds
                + ", topK=" + topK
                + ", queryVectorDimension=" + queryVector.length
                + ", timeout=" + timeout
                + "]";
    }
}
