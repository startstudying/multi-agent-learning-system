package com.learningos.rag.parser;

public record ParseInput(
        String documentId,
        String name,
        String contentType,
        long sizeBytes,
        byte[] bytes
) {
    public ParseInput {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
