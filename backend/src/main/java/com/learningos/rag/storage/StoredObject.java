package com.learningos.rag.storage;

public record StoredObject(
        String bucket,
        String key,
        long sizeBytes,
        String contentType
) {
}
