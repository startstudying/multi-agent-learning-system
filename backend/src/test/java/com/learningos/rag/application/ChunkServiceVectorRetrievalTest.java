package com.learningos.rag.application;

import com.learningos.config.AiModelProperties;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.repository.KbDocChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChunkServiceVectorRetrievalTest {

    @Test
    void filtersVectorCandidatesAgainstAllowedKnowledgeBasesBeforeRrfFusion() {
        KbDocChunkRepository repository = mock(KbDocChunkRepository.class);
        when(repository.findTop20ByKbIdInOrderByCreatedAtDesc(anyCollection())).thenReturn(List.of());
        KbDocChunk allowed = chunk("chunk_allowed", "kb_allowed", "Allowed vector hit");
        KbDocChunk forbidden = chunk("chunk_forbidden", "kb_forbidden", "Forbidden vector hit");
        when(repository.findAllById(List.of("chunk_forbidden", "chunk_allowed"))).thenReturn(List.of(forbidden, allowed));
        VectorIndexAdapter adapter = new FakeVectorIndexAdapter(VectorSearchResult.succeeded(List.of(
                new VectorSearchHit("chunk_forbidden", 0.99),
                new VectorSearchHit("chunk_allowed", 0.98)
        )));
        EmbeddingService embeddingService = embeddingServiceWithVector(0.4f, 0.5f, 0.6f);
        ChunkService service = new ChunkService(repository, adapter, embeddingService);

        RetrievalResult result = service.retrieveAllowedChunks(
                List.of("kb_allowed"),
                "vector question",
                5
        );

        assertThat(result.vectorEnabled()).isTrue();
        assertThat(result.vectorCandidateCount()).isEqualTo(1);
        assertThat(result.chunks())
                .extracting(KbDocChunk::getId)
                .containsExactly("chunk_allowed");
    }

    @Test
    void fallsBackToKeywordAndRecencyWhenVectorSearchReportsProviderError() {
        KbDocChunk recency = chunk("chunk_recent", "kb_allowed", "Recent fallback content");
        KbDocChunkRepository repository = mock(KbDocChunkRepository.class);
        when(repository.findTop20ByKbIdInOrderByCreatedAtDesc(anyCollection())).thenReturn(List.of(recency));
        VectorIndexAdapter adapter = new FakeVectorIndexAdapter(new VectorSearchResult(
                VectorIndexStatus.PROVIDER_ERROR,
                List.of(new VectorSearchHit("chunk_vector", 0.95)),
                3L,
                "VECTOR_PROVIDER_ERROR"
        ));
        EmbeddingService embeddingService = embeddingServiceWithVector(0.7f, 0.8f, 0.9f);
        ChunkService service = new ChunkService(repository, adapter, embeddingService);

        RetrievalResult result = service.retrieveAllowedChunks(
                List.of("kb_allowed"),
                "unmatched query",
                5
        );

        assertThat(result.vectorEnabled()).isFalse();
        assertThat(result.vectorCandidateCount()).isZero();
        assertThat(result.chunks())
                .extracting(KbDocChunk::getId)
                .containsExactly("chunk_recent");
    }

    @Test
    void usesQueryEmbeddingVectorForVectorSearchRequestWithoutPassingRawQuestion() {
        KbDocChunkRepository repository = mock(KbDocChunkRepository.class);
        when(repository.findTop20ByKbIdInOrderByCreatedAtDesc(anyCollection())).thenReturn(List.of());
        KbDocChunk allowed = chunk("chunk_allowed", "kb_allowed", "Allowed vector hit");
        when(repository.findAllById(List.of("chunk_allowed"))).thenReturn(List.of(allowed));
        AtomicReference<VectorSearchRequest> capturedVectorRequest = new AtomicReference<>();
        VectorIndexAdapter adapter = new FakeVectorIndexAdapter(
                VectorSearchResult.succeeded(List.of(new VectorSearchHit("chunk_allowed", 0.98))),
                capturedVectorRequest
        );
        AtomicReference<EmbeddingRequest> capturedEmbeddingRequest = new AtomicReference<>();
        EmbeddingService embeddingService = new EmbeddingService(
                new AiModelProperties("openai", "", "embed-test"),
                new FakeEmbeddingModel(capturedEmbeddingRequest, new EmbeddingResponse(
                        List.of(new Embedding(new float[]{0.4f, 0.5f, 0.6f}, 0)),
                        new EmbeddingResponseMetadata("embed-test", new FixedUsage(5, 0))
                ))
        );
        ChunkService service = new ChunkService(repository, adapter, embeddingService);

        RetrievalResult result = service.retrieveAllowedChunks(
                List.of("kb_allowed"),
                "vector question with secret-ish text",
                5
        );

        assertThat(result.vectorEnabled()).isTrue();
        assertThat(capturedEmbeddingRequest.get().getInstructions())
                .containsExactly("vector question with secret-ish text");
        assertThat(capturedVectorRequest.get().allowedKbIds()).containsExactly("kb_allowed");
        assertThat(capturedVectorRequest.get().topK()).isEqualTo(5);
        assertThat(capturedVectorRequest.get().queryVector()).isNotNull();
        assertThat(capturedVectorRequest.get().queryVector().values()).containsExactly(0.4f, 0.5f, 0.6f);
        assertThat(capturedVectorRequest.get().toString())
                .doesNotContain("vector question", "secret-ish", "0.4", "0.5", "0.6");
    }

    @Test
    void fallsBackWithoutCallingVectorAdapterWhenQueryEmbeddingFails() {
        KbDocChunk recency = chunk("chunk_recent", "kb_allowed", "Recent fallback content");
        KbDocChunkRepository repository = mock(KbDocChunkRepository.class);
        when(repository.findTop20ByKbIdInOrderByCreatedAtDesc(anyCollection())).thenReturn(List.of(recency));
        CountingVectorIndexAdapter adapter = new CountingVectorIndexAdapter();
        EmbeddingService embeddingService = new EmbeddingService(new AiModelProperties("openai", "", "embed-test"));
        ChunkService service = new ChunkService(repository, adapter, embeddingService);

        RetrievalResult result = service.retrieveAllowedChunks(
                List.of("kb_allowed"),
                "unmatched query",
                5
        );

        assertThat(adapter.searchCalls()).isZero();
        assertThat(result.vectorEnabled()).isFalse();
        assertThat(result.vectorCandidateCount()).isZero();
        assertThat(result.chunks())
                .extracting(KbDocChunk::getId)
                .containsExactly("chunk_recent");
    }

    private KbDocChunk chunk(String id, String kbId, String content) {
        KbDocChunk chunk = new KbDocChunk();
        chunk.setId(id);
        chunk.setKbId(kbId);
        chunk.setDocumentId("doc_" + id);
        chunk.setDocumentVersion(1);
        chunk.setChunkIndex(0);
        chunk.setContent(content);
        chunk.setSectionTitle(id);
        chunk.setCreatedAt(Instant.now());
        return chunk;
    }

    private EmbeddingService embeddingServiceWithVector(float first, float second, float third) {
        return new EmbeddingService(
                new AiModelProperties("openai", "", "embed-test"),
                new FakeEmbeddingModel(new AtomicReference<>(), new EmbeddingResponse(
                        List.of(new Embedding(new float[]{first, second, third}, 0)),
                        new EmbeddingResponseMetadata("embed-test", new FixedUsage(5, 0))
                ))
        );
    }

    private static final class FakeVectorIndexAdapter implements VectorIndexAdapter {
        private final VectorSearchResult result;
        private final AtomicReference<VectorSearchRequest> capturedRequest;

        private FakeVectorIndexAdapter(VectorSearchResult result) {
            this(result, null);
        }

        private FakeVectorIndexAdapter(VectorSearchResult result, AtomicReference<VectorSearchRequest> capturedRequest) {
            this.result = result;
            this.capturedRequest = capturedRequest;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public VectorUpsertResult deleteDocument(String kbId, String documentId, int documentVersion) {
            return new VectorUpsertResult(VectorIndexStatus.SUCCEEDED, 0, 0L, null);
        }

        @Override
        public VectorUpsertResult upsert(VectorUpsertRequest request) {
            return new VectorUpsertResult(VectorIndexStatus.SUCCEEDED, 0, 0L, null);
        }

        @Override
        public VectorSearchResult search(VectorSearchRequest request) {
            if (capturedRequest != null) {
                capturedRequest.set(request);
            }
            assertThat(request.allowedKbIds()).containsExactly("kb_allowed");
            return result;
        }
    }

    private static final class CountingVectorIndexAdapter implements VectorIndexAdapter {
        private final AtomicInteger searchCalls = new AtomicInteger();

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public VectorUpsertResult deleteDocument(String kbId, String documentId, int documentVersion) {
            return new VectorUpsertResult(VectorIndexStatus.SUCCEEDED, 0, 0L, null);
        }

        @Override
        public VectorUpsertResult upsert(VectorUpsertRequest request) {
            return new VectorUpsertResult(VectorIndexStatus.SUCCEEDED, 0, 0L, null);
        }

        @Override
        public VectorSearchResult search(VectorSearchRequest request) {
            searchCalls.incrementAndGet();
            return VectorSearchResult.succeeded(List.of());
        }

        int searchCalls() {
            return searchCalls.get();
        }
    }

    private static class FakeEmbeddingModel implements EmbeddingModel {

        private final AtomicReference<EmbeddingRequest> capturedRequest;
        private final EmbeddingResponse response;

        private FakeEmbeddingModel(AtomicReference<EmbeddingRequest> capturedRequest, EmbeddingResponse response) {
            this.capturedRequest = capturedRequest;
            this.response = response;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            capturedRequest.set(request);
            return response;
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return new float[0];
        }
    }

    private record FixedUsage(Integer promptTokens, Integer completionTokens) implements Usage {

        @Override
        public Integer getPromptTokens() {
            return promptTokens;
        }

        @Override
        public Integer getCompletionTokens() {
            return completionTokens;
        }

        @Override
        public Object getNativeUsage() {
            return null;
        }
    }
}
