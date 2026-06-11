package com.learningos.rag.application;

import com.learningos.config.RagProperties;
import com.learningos.rag.domain.KbDocChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class RerankerService {

    private final RagProperties ragProperties;

    public RerankerService(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public List<KbDocChunk> rerankOrFallback(List<KbDocChunk> chunks) {
        return rerankOrFallback("", chunks, chunks == null ? 0 : chunks.size()).chunks();
    }

    public RerankResult rerankOrFallback(String question, List<KbDocChunk> chunks, int topK) {
        List<KbDocChunk> safeChunks = chunks == null ? List.of() : limit(chunks, topK);
        if (safeChunks.isEmpty()) {
            return new RerankResult(safeChunks, RerankerStatus.SKIPPED_NO_CANDIDATES, false, 0L, null);
        }
        if (!hasConfiguredReranker()) {
            return new RerankResult(safeChunks, RerankerStatus.NOT_CONFIGURED, false, 0L, null);
        }

        long startedAt = System.nanoTime();
        CompletableFuture<List<KbDocChunk>> future = CompletableFuture.supplyAsync(
                () -> invokeReranker(question, safeChunks, topK)
        );
        try {
            List<KbDocChunk> reranked = future.get(ragProperties.rerankerTimeoutMs(), TimeUnit.MILLISECONDS);
            return new RerankResult(
                    limit(reranked == null ? safeChunks : reranked, topK),
                    RerankerStatus.SUCCEEDED,
                    false,
                    elapsedMs(startedAt),
                    null
            );
        } catch (TimeoutException exception) {
            future.cancel(true);
            return new RerankResult(
                    safeChunks,
                    RerankerStatus.TIMEOUT_FALLBACK,
                    true,
                    elapsedMs(startedAt),
                    "RERANKER_TIMEOUT"
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new RerankResult(
                    safeChunks,
                    RerankerStatus.ERROR_FALLBACK,
                    true,
                    elapsedMs(startedAt),
                    "RERANKER_ERROR"
            );
        } catch (ExecutionException exception) {
            return new RerankResult(
                    safeChunks,
                    RerankerStatus.ERROR_FALLBACK,
                    true,
                    elapsedMs(startedAt),
                    "RERANKER_ERROR"
            );
        }
    }

    protected boolean hasConfiguredReranker() {
        return false;
    }

    protected List<KbDocChunk> invokeReranker(String question, List<KbDocChunk> chunks, int topK) {
        return chunks;
    }

    private List<KbDocChunk> limit(List<KbDocChunk> chunks, int topK) {
        int limit = Math.max(1, topK);
        return chunks.stream().limit(limit).toList();
    }

    private long elapsedMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
