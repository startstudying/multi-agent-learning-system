package com.learningos.rag.vector;

import java.time.Duration;

public record QdrantVectorDeleteCommand(
        String collectionName,
        String kbId,
        String documentId,
        int documentVersion,
        Duration timeout
) {
}
