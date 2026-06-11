package com.learningos.rag.application;

import com.learningos.rag.domain.KbDocChunk;

import java.util.List;

public record RetrievalResult(
        List<KbDocChunk> chunks,
        String retrievalMode,
        int keywordCandidateCount,
        int recencyCandidateCount,
        int vectorCandidateCount,
        boolean vectorEnabled,
        int fusedCandidateCount,
        int rrfK
) {
}
