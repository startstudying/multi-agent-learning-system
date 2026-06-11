package com.learningos.rag.application;

import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.repository.KbDocChunkRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ChunkService {

    private static final int MAX_TOP_K = 20;
    private static final int RRF_K = 60;
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "why", "how", "does", "what", "this", "that",
            "rows", "row", "about", "using", "use", "explain", "diagnose"
    );

    private final KbDocChunkRepository chunkRepository;
    private final VectorIndexAdapter vectorIndexAdapter;
    private final EmbeddingService embeddingService;
    private final RrfRanker rrfRanker = new RrfRanker();

    public ChunkService(
            KbDocChunkRepository chunkRepository,
            VectorIndexAdapter vectorIndexAdapter,
            EmbeddingService embeddingService
    ) {
        this.chunkRepository = chunkRepository;
        this.vectorIndexAdapter = vectorIndexAdapter;
        this.embeddingService = embeddingService;
    }

    public List<KbDocChunk> retrieveAllowedChunks(List<String> allowedKbIds, int topK) {
        return retrieveAllowedChunks(allowedKbIds, "", topK).chunks();
    }

    public RetrievalResult retrieveAllowedChunks(List<String> allowedKbIds, String question, int topK) {
        if (allowedKbIds == null || allowedKbIds.isEmpty()) {
            return emptyResult();
        }

        int limit = safeLimit(topK);
        List<KbDocChunk> recencyCandidates = chunkRepository.findTop20ByKbIdInOrderByCreatedAtDesc(allowedKbIds);
        List<KbDocChunk> keywordCandidates = keywordCandidates(question, recencyCandidates);
        VectorSearchResult vectorSearch = vectorSearch(allowedKbIds, question, limit);
        List<KbDocChunk> vectorCandidates = allowedVectorCandidates(allowedKbIds, vectorSearch);
        boolean vectorEnabled = vectorSearch.status() == VectorIndexStatus.SUCCEEDED && !vectorCandidates.isEmpty();
        List<List<KbDocChunk>> branches = new java.util.ArrayList<>();
        branches.add(keywordCandidates);
        branches.add(recencyCandidates);
        if (vectorEnabled) {
            branches.add(vectorCandidates);
        }
        List<KbDocChunk> fused = rrfRanker.fuse(
                branches,
                limit,
                RRF_K
        );

        return new RetrievalResult(
                fused,
                "HYBRID_RRF",
                keywordCandidates.size(),
                recencyCandidates.size(),
                vectorCandidates.size(),
                vectorEnabled,
                fused.size(),
                RRF_K
        );
    }

    private VectorSearchResult vectorSearch(List<String> allowedKbIds, String question, int topK) {
        if (!vectorIndexAdapter.isEnabled() || !embeddingService.isEnabled()) {
            return VectorSearchResult.disabled();
        }
        long startedAt = System.nanoTime();
        try {
            QueryEmbeddingResult queryEmbedding = embeddingService.embedQuery(question);
            if (queryEmbedding.status() != EmbeddingStatus.SUCCEEDED || queryEmbedding.vector() == null) {
                return VectorSearchResult.disabled();
            }
            return vectorIndexAdapter.search(new VectorSearchRequest(allowedKbIds, topK, queryEmbedding.vector()));
        } catch (RuntimeException exception) {
            return new VectorSearchResult(
                    VectorIndexStatus.PROVIDER_ERROR,
                    List.of(),
                    Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)),
                    "VECTOR_SEARCH_FAILED"
            );
        }
    }

    private List<KbDocChunk> allowedVectorCandidates(List<String> allowedKbIds, VectorSearchResult vectorSearch) {
        if (vectorSearch.status() != VectorIndexStatus.SUCCEEDED || vectorSearch.hits() == null) {
            return List.of();
        }
        Set<String> allowed = allowedKbIds == null ? Set.of() : new LinkedHashSet<>(allowedKbIds);
        List<String> hitChunkIds = vectorSearch.hits().stream()
                .filter(Objects::nonNull)
                .map(VectorSearchHit::chunkId)
                .filter(chunkId -> chunkId != null && !chunkId.isBlank())
                .distinct()
                .toList();
        if (hitChunkIds.isEmpty()) {
            return List.of();
        }
        Map<String, KbDocChunk> chunksById = chunkRepository.findAllById(hitChunkIds).stream()
                .filter(chunk -> chunk != null && allowed.contains(chunk.getKbId()))
                .collect(Collectors.toMap(KbDocChunk::getId, Function.identity(), (left, right) -> left));
        return hitChunkIds.stream()
                .map(chunksById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<KbDocChunk> keywordCandidates(String question, List<KbDocChunk> candidates) {
        Set<String> tokens = queryTokens(question);
        if (tokens.isEmpty() || candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .map(chunk -> new ScoredChunk(chunk, keywordScore(tokens, chunk)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator
                        .comparingInt(ScoredChunk::score)
                        .reversed()
                        .thenComparing(scored -> createdAt(scored.chunk()), Comparator.reverseOrder())
                        .thenComparing(scored -> scored.chunk().getChunkIndex(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(scored -> scored.chunk().getId(), Comparator.nullsLast(String::compareTo)))
                .map(ScoredChunk::chunk)
                .toList();
    }

    private int keywordScore(Set<String> tokens, KbDocChunk chunk) {
        String content = normalize(chunk.getContent());
        String sectionTitle = normalize(chunk.getSectionTitle());
        int score = 0;
        for (String token : tokens) {
            if (!sectionTitle.isBlank() && sectionTitle.contains(token)) {
                score += 2;
            }
            if (!content.isBlank() && content.contains(token)) {
                score += 1;
            }
        }
        return score;
    }

    private Set<String> queryTokens(String question) {
        if (question == null || question.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalize(question).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private Instant createdAt(KbDocChunk chunk) {
        return chunk.getCreatedAt() == null ? Instant.EPOCH : chunk.getCreatedAt();
    }

    private int safeLimit(int topK) {
        return Math.min(MAX_TOP_K, Math.max(1, topK));
    }

    private RetrievalResult emptyResult() {
        return new RetrievalResult(
                List.of(),
                "HYBRID_RRF",
                0,
                0,
                0,
                false,
                0,
                RRF_K
        );
    }

    private record ScoredChunk(KbDocChunk chunk, int score) {
    }
}
