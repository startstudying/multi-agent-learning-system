package com.learningos.rag.application;

import com.learningos.rag.domain.KbDocChunk;

import java.util.List;

public record RerankResult(
        List<KbDocChunk> chunks,
        RerankerStatus status,
        boolean fallbackUsed,
        Long latencyMs,
        String errorCode
) {
}
