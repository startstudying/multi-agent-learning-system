package com.learningos.rag.application;

import com.learningos.rag.domain.KbDocChunk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record PocContextResult(
        List<KbDocChunk> contextChunks,
        int sourceChunkCount,
        int parentChunkCount,
        int adjacentChunkCount,
        int childChunkCount
) {

    static PocContextResult empty() {
        return new PocContextResult(List.of(), 0, 0, 0, 0);
    }

    int contextChunkCount() {
        return contextChunks == null ? 0 : contextChunks.size();
    }

    int expandedChunkCount() {
        return Math.max(0, contextChunkCount() - sourceChunkCount);
    }

    Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("enabled", sourceChunkCount > 0);
        metadata.put("sourceChunkCount", sourceChunkCount);
        metadata.put("contextChunkCount", contextChunkCount());
        metadata.put("expandedChunkCount", expandedChunkCount());
        metadata.put("parentChunkCount", parentChunkCount);
        metadata.put("adjacentChunkCount", adjacentChunkCount);
        metadata.put("childChunkCount", childChunkCount);
        return metadata;
    }
}
