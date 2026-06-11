package com.learningos.rag.application;

import com.learningos.config.AiModelProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    @Test
    void returnsDisabledBatchResultWhenEmbeddingProviderIsNotConfigured() {
        EmbeddingService service = new EmbeddingService(new AiModelProperties("none", "", ""));

        EmbeddingBatchResult result = service.embedDocumentChunks(
                "doc_1",
                "kb_1",
                List.of(new ChunkEmbeddingInput("chunk_1", "SQL JOIN duplicates"))
        );

        assertThat(service.isEnabled()).isFalse();
        assertThat(service.currentModelVersion()).isEqualTo("noop-embedding-v1");
        assertThat(result.status()).isEqualTo(EmbeddingStatus.DISABLED);
        assertThat(result.modelVersion()).isEqualTo("noop-embedding-v1");
        assertThat(result.chunkCount()).isEqualTo(1);
        assertThat(result.errorCode()).isNull();
    }

    @Test
    void reportsConfiguredEmbeddingModelWithoutCallingExternalProvider() {
        EmbeddingService service = new EmbeddingService(new AiModelProperties("openai", "gpt-test", "text-embedding-test"));

        assertThat(service.isEnabled()).isTrue();
        assertThat(service.currentModelVersion()).isEqualTo("text-embedding-test");

        EmbeddingBatchResult result = service.embedDocumentChunks(
                "doc_1",
                "kb_1",
                List.of(new ChunkEmbeddingInput("chunk_1", "SQL JOIN duplicates"))
        );

        assertThat(result.status()).isEqualTo(EmbeddingStatus.PROVIDER_ERROR);
        assertThat(result.errorCode()).isEqualTo("EMBEDDING_PROVIDER_NOT_CONFIGURED");
        assertThat(result.modelVersion()).isEqualTo("text-embedding-test");
    }

    @Test
    void callsRealEmbeddingModelAdapterWhenProviderAndEmbeddingModelAreConfigured() {
        AtomicReference<EmbeddingRequest> capturedRequest = new AtomicReference<>();
        EmbeddingModel embeddingModel = new FakeEmbeddingModel(capturedRequest, new EmbeddingResponse(
                List.of(new Embedding(new float[]{0.1f, 0.2f, 0.3f}, 0)),
                new EmbeddingResponseMetadata("text-embedding-provider-returned", new FixedUsage(11, 0))
        ));
        EmbeddingService service = new EmbeddingService(
                new AiModelProperties("openai", "gpt-test", "text-embedding-test"),
                embeddingModel
        );

        EmbeddingBatchResult result = service.embedDocumentChunks(
                "doc_1",
                "kb_1",
                List.of(new ChunkEmbeddingInput("chunk_1", "SQL JOIN duplicates"))
        );

        assertThat(result.status()).isEqualTo(EmbeddingStatus.SUCCEEDED);
        assertThat(result.errorCode()).isNull();
        assertThat(result.modelVersion()).isEqualTo("text-embedding-provider-returned");
        assertThat(result.chunkCount()).isEqualTo(1);
        assertThat(result.vectors()).hasSize(1);
        assertThat(result.vectors().getFirst().ownerId()).isEqualTo("chunk_1");
        assertThat(result.vectors().getFirst().values()).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(capturedRequest.get().getInstructions()).containsExactly("SQL JOIN duplicates");
    }

    @Test
    void createsQueryEmbeddingVectorWithoutLeakingRawVectorInStringRepresentation() {
        AtomicReference<EmbeddingRequest> capturedRequest = new AtomicReference<>();
        EmbeddingModel embeddingModel = new FakeEmbeddingModel(capturedRequest, new EmbeddingResponse(
                List.of(new Embedding(new float[]{0.4f, 0.5f, 0.6f}, 0)),
                new EmbeddingResponseMetadata("text-embedding-provider-returned", new FixedUsage(7, 0))
        ));
        EmbeddingService service = new EmbeddingService(
                new AiModelProperties("openai", "gpt-test", "text-embedding-test"),
                embeddingModel
        );

        QueryEmbeddingResult result = service.embedQuery("How does vector search work?");

        assertThat(result.status()).isEqualTo(EmbeddingStatus.SUCCEEDED);
        assertThat(result.errorCode()).isNull();
        assertThat(result.modelVersion()).isEqualTo("text-embedding-provider-returned");
        assertThat(result.vector()).isNotNull();
        assertThat(result.vector().ownerId()).isEqualTo("query");
        assertThat(result.vector().values()).containsExactly(0.4f, 0.5f, 0.6f);
        assertThat(result.vector().toString())
                .contains("ownerId=query", "dimension=3")
                .doesNotContain("0.4", "0.5", "0.6", "How does vector");
        assertThat(capturedRequest.get().getInstructions()).containsExactly("How does vector search work?");
    }

    @Test
    void configuredEmbeddingProviderWithoutBeanFailsClosed() {
        EmbeddingService service = new EmbeddingService(new AiModelProperties("openai", "gpt-test", "text-embedding-test"));

        EmbeddingBatchResult result = service.embedDocumentChunks(
                "doc_1",
                "kb_1",
                List.of(new ChunkEmbeddingInput("chunk_1", "SQL JOIN duplicates"))
        );

        assertThat(result.status()).isEqualTo(EmbeddingStatus.PROVIDER_ERROR);
        assertThat(result.errorCode()).isEqualTo("EMBEDDING_PROVIDER_NOT_CONFIGURED");
    }

    @Test
    void sanitizesRawEmbeddingProviderError() {
        EmbeddingModel embeddingModel = new FakeEmbeddingModel(null, null) {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                throw new IllegalStateException("raw provider sk-live-secret chunk SQL JOIN duplicates");
            }
        };
        EmbeddingService service = new EmbeddingService(
                new AiModelProperties("openai", "gpt-test", "text-embedding-test"),
                embeddingModel
        );

        EmbeddingBatchResult result = service.embedDocumentChunks(
                "doc_1",
                "kb_1",
                List.of(new ChunkEmbeddingInput("chunk_1", "SQL JOIN duplicates"))
        );

        assertThat(result.status()).isEqualTo(EmbeddingStatus.PROVIDER_ERROR);
        assertThat(result.errorCode()).isEqualTo("EMBEDDING_PROVIDER_ERROR");
        assertThat(result.errorCode()).doesNotContain("sk-live-secret", "SQL JOIN");
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
            if (capturedRequest != null) {
                capturedRequest.set(request);
            }
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
