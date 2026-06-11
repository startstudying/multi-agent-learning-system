package com.learningos.rag.application;

import java.util.List;

public record VectorUpsertRequest(
        String kbId,
        String documentId,
        int documentVersion,
        List<VectorChunkReference> chunks
) {
}
