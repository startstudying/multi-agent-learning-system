package com.learningos.rag.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.repository.KbDocChunkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class PocContextBuilder {

    private static final int MAX_CONTEXT_CHUNKS = 60;

    private final KbDocChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;

    public PocContextBuilder(KbDocChunkRepository chunkRepository, ObjectMapper objectMapper) {
        this.chunkRepository = chunkRepository;
        this.objectMapper = objectMapper;
    }

    public PocContextResult build(List<String> allowedKbIds, List<KbDocChunk> sourceChunks) {
        if (sourceChunks == null || sourceChunks.isEmpty()) {
            return PocContextResult.empty();
        }
        Set<String> allowed = allowedSet(allowedKbIds);
        if (allowed.isEmpty()) {
            return PocContextResult.empty();
        }

        ContextAccumulator accumulator = new ContextAccumulator();
        Map<String, List<KbDocChunk>> documentCache = new HashMap<>();
        for (KbDocChunk source : sourceChunks) {
            if (!isAllowed(source, allowed) || source.getId() == null || source.getDocumentId() == null) {
                continue;
            }
            List<KbDocChunk> documentChunks = documentCache.computeIfAbsent(
                    source.getDocumentId(),
                    chunkRepository::findByDocumentIdOrderByChunkIndex
            ).stream()
                    .filter(chunk -> isAllowed(chunk, allowed))
                    .filter(chunk -> sameDocumentVersion(source, chunk))
                    .sorted(Comparator.comparing(KbDocChunk::getChunkIndex, Comparator.nullsLast(Integer::compareTo)))
                    .toList();

            parentChain(source, documentChunks).forEach(parent -> accumulator.add(parent, ContextReason.PARENT));
            neighbor(source, documentChunks, -1).ifPresent(previous -> accumulator.add(previous, ContextReason.ADJACENT));
            accumulator.add(source, ContextReason.OWN);
            firstChild(source, documentChunks).ifPresent(child -> accumulator.add(child, ContextReason.CHILD));
            neighbor(source, documentChunks, 1).ifPresent(next -> accumulator.add(next, ContextReason.ADJACENT));
        }

        return accumulator.toResult();
    }

    private Set<String> allowedSet(List<String> allowedKbIds) {
        if (allowedKbIds == null) {
            return Set.of();
        }
        Set<String> allowed = new LinkedHashSet<>();
        allowedKbIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(kbId -> !kbId.isEmpty())
                .forEach(allowed::add);
        return allowed;
    }

    private List<KbDocChunk> parentChain(KbDocChunk source, List<KbDocChunk> documentChunks) {
        List<String> sourcePath = headingPath(source);
        if (sourcePath.size() < 2) {
            return List.of();
        }
        Integer sourceIndex = source.getChunkIndex();
        Map<String, KbDocChunk> nearestByPath = new LinkedHashMap<>();
        documentChunks.stream()
                .filter(chunk -> before(chunk, sourceIndex))
                .filter(chunk -> {
                    List<String> candidatePath = headingPath(chunk);
                    return candidatePath.size() < sourcePath.size() && startsWith(sourcePath, candidatePath);
                })
                .sorted(Comparator.comparing(KbDocChunk::getChunkIndex, Comparator.nullsLast(Integer::compareTo)))
                .forEach(chunk -> nearestByPath.put(String.join("\u001F", headingPath(chunk)), chunk));
        return nearestByPath.values().stream()
                .sorted(Comparator.comparing(KbDocChunk::getChunkIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private Optional<KbDocChunk> firstChild(KbDocChunk source, List<KbDocChunk> documentChunks) {
        List<String> sourcePath = headingPath(source);
        if (sourcePath.isEmpty()) {
            return Optional.empty();
        }
        Integer sourceIndex = source.getChunkIndex();
        return documentChunks.stream()
                .filter(chunk -> after(chunk, sourceIndex))
                .filter(chunk -> {
                    List<String> candidatePath = headingPath(chunk);
                    return candidatePath.size() > sourcePath.size() && startsWith(candidatePath, sourcePath);
                })
                .findFirst();
    }

    private Optional<KbDocChunk> neighbor(KbDocChunk source, List<KbDocChunk> documentChunks, int offset) {
        if (source.getChunkIndex() == null) {
            return Optional.empty();
        }
        int targetIndex = source.getChunkIndex() + offset;
        return documentChunks.stream()
                .filter(chunk -> Objects.equals(chunk.getChunkIndex(), targetIndex))
                .findFirst();
    }

    private List<String> headingPath(KbDocChunk chunk) {
        if (chunk == null || chunk.getMetadataJson() == null || chunk.getMetadataJson().isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    chunk.getMetadataJson(),
                    new TypeReference<>() {
                    }
            );
            Object headingPath = metadata.get("headingPath");
            if (!(headingPath instanceof List<?> values)) {
                return List.of();
            }
            List<String> normalized = new ArrayList<>();
            for (Object value : values) {
                if (value != null && !value.toString().isBlank()) {
                    normalized.add(value.toString().trim());
                }
            }
            return normalized;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private boolean startsWith(List<String> value, List<String> prefix) {
        if (prefix.isEmpty() || value.size() < prefix.size()) {
            return false;
        }
        for (int index = 0; index < prefix.size(); index++) {
            if (!Objects.equals(value.get(index), prefix.get(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean before(KbDocChunk chunk, Integer sourceIndex) {
        return sourceIndex != null && chunk.getChunkIndex() != null && chunk.getChunkIndex() < sourceIndex;
    }

    private boolean after(KbDocChunk chunk, Integer sourceIndex) {
        return sourceIndex != null && chunk.getChunkIndex() != null && chunk.getChunkIndex() > sourceIndex;
    }

    private boolean isAllowed(KbDocChunk chunk, Set<String> allowedKbIds) {
        return chunk != null && chunk.getKbId() != null && allowedKbIds.contains(chunk.getKbId());
    }

    private boolean sameDocumentVersion(KbDocChunk source, KbDocChunk candidate) {
        return Objects.equals(source.getDocumentVersion(), candidate.getDocumentVersion());
    }

    private enum ContextReason {
        OWN,
        PARENT,
        ADJACENT,
        CHILD
    }

    private static final class ContextAccumulator {
        private final Map<String, KbDocChunk> chunksById = new LinkedHashMap<>();
        private final Set<String> sourceIds = new LinkedHashSet<>();
        private final Set<String> parentIds = new LinkedHashSet<>();
        private final Set<String> adjacentIds = new LinkedHashSet<>();
        private final Set<String> childIds = new LinkedHashSet<>();

        private void add(KbDocChunk chunk, ContextReason reason) {
            if (chunk == null || chunk.getId() == null) {
                return;
            }
            if (chunksById.size() >= MAX_CONTEXT_CHUNKS && !chunksById.containsKey(chunk.getId())) {
                return;
            }
            chunksById.putIfAbsent(chunk.getId(), chunk);
            switch (reason) {
                case OWN -> sourceIds.add(chunk.getId());
                case PARENT -> parentIds.add(chunk.getId());
                case ADJACENT -> adjacentIds.add(chunk.getId());
                case CHILD -> childIds.add(chunk.getId());
            }
        }

        private PocContextResult toResult() {
            return new PocContextResult(
                    List.copyOf(chunksById.values()),
                    sourceIds.size(),
                    parentIds.size(),
                    adjacentIds.size(),
                    childIds.size()
            );
        }
    }
}
