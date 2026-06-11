package com.learningos.rag.vector;

import java.time.Duration;
import java.util.List;

public record QdrantVectorUpsertCommand(
        String collectionName,
        List<QdrantVectorPoint> points,
        Duration timeout
) {
    public QdrantVectorUpsertCommand {
        points = points == null ? List.of() : List.copyOf(points);
    }

    @Override
    public String toString() {
        return "QdrantVectorUpsertCommand[collectionName=" + collectionName
                + ", pointCount=" + points.size()
                + ", timeout=" + timeout
                + "]";
    }
}
