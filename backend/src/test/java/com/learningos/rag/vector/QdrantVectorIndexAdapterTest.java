package com.learningos.rag.vector;

import com.learningos.config.RagVectorProperties;
import com.learningos.rag.application.EmbeddingVector;
import com.learningos.rag.application.VectorChunkReference;
import com.learningos.rag.application.VectorIndexStatus;
import com.learningos.rag.application.VectorSearchHit;
import com.learningos.rag.application.VectorSearchRequest;
import com.learningos.rag.application.VectorUpsertRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantVectorIndexAdapterTest {

    @Test
    void upsertMapsVectorPayloadWithoutRawChunkContent() {
        CapturingOperations operations = new CapturingOperations();
        QdrantVectorIndexAdapter adapter = adapter(operations);

        var result = adapter.upsert(new VectorUpsertRequest(
                "kb_1",
                "doc_1",
                2,
                List.of(new VectorChunkReference(
                        "chunk_1",
                        "hash_1",
                        0,
                        new EmbeddingVector("chunk_1", new float[]{0.11f, 0.22f})
                ))
        ));

        assertThat(result.status()).isEqualTo(VectorIndexStatus.SUCCEEDED);
        assertThat(result.upsertCount()).isEqualTo(1);
        QdrantVectorUpsertCommand command = operations.upsertCommand.get();
        assertThat(command.collectionName()).isEqualTo("learning_os_chunks");
        assertThat(command.points()).hasSize(1);
        QdrantVectorPoint point = command.points().getFirst();
        assertThat(point.chunkId()).isEqualTo("chunk_1");
        assertThat(point.kbId()).isEqualTo("kb_1");
        assertThat(point.documentId()).isEqualTo("doc_1");
        assertThat(point.documentVersion()).isEqualTo(2);
        assertThat(point.chunkHash()).isEqualTo("hash_1");
        assertThat(point.chunkIndex()).isZero();
        assertThat(point.vector()).containsExactly(0.11f, 0.22f);
        assertThat(command.toString())
                .doesNotContain("raw chunk", "question", "prompt", "storage", "userId", "secret", "0.11", "0.22");
    }

    @Test
    void searchPassesAllowedKbIdsTopKAndQueryVectorWithoutRawQuestion() {
        CapturingOperations operations = new CapturingOperations();
        operations.searchResult = List.of(new QdrantVectorSearchHit("chunk_1", 0.91));
        QdrantVectorIndexAdapter adapter = adapter(operations);

        var result = adapter.search(new VectorSearchRequest(
                List.of("kb_1", "kb_2"),
                7,
                new EmbeddingVector("query", new float[]{0.33f, 0.44f})
        ));

        assertThat(result.status()).isEqualTo(VectorIndexStatus.SUCCEEDED);
        assertThat(result.hits()).containsExactly(new VectorSearchHit("chunk_1", 0.91));
        QdrantVectorSearchCommand command = operations.searchCommand.get();
        assertThat(command.collectionName()).isEqualTo("learning_os_chunks");
        assertThat(command.allowedKbIds()).containsExactly("kb_1", "kb_2");
        assertThat(command.topK()).isEqualTo(7);
        assertThat(command.queryVector()).containsExactly(0.33f, 0.44f);
        assertThat(command.toString()).doesNotContain("0.33", "0.44", "raw question");
    }

    @Test
    void providerFailuresReturnSafeErrorCodes() {
        CapturingOperations operations = new CapturingOperations();
        operations.fail = true;
        QdrantVectorIndexAdapter adapter = adapter(operations);

        var upsert = adapter.upsert(new VectorUpsertRequest(
                "kb_1",
                "doc_1",
                1,
                List.of(new VectorChunkReference(
                        "chunk_1",
                        "hash_1",
                        0,
                        new EmbeddingVector("chunk_1", new float[]{0.1f})
                ))
        ));
        var search = adapter.search(new VectorSearchRequest(
                List.of("kb_1"),
                5,
                new EmbeddingVector("query", new float[]{0.2f})
        ));
        var delete = adapter.deleteDocument("kb_1", "doc_1", 1);

        assertThat(upsert.status()).isEqualTo(VectorIndexStatus.VECTOR_UPSERT_FAILED);
        assertThat(upsert.errorCode()).isEqualTo("VECTOR_UPSERT_FAILED");
        assertThat(search.status()).isEqualTo(VectorIndexStatus.PROVIDER_ERROR);
        assertThat(search.errorCode()).isEqualTo("VECTOR_SEARCH_FAILED");
        assertThat(delete.status()).isEqualTo(VectorIndexStatus.VECTOR_UPSERT_FAILED);
        assertThat(delete.errorCode()).isEqualTo("VECTOR_DELETE_FAILED");
        assertThat(upsert.toString()).doesNotContain("sk-live-secret", "raw chunk");
    }

    private QdrantVectorIndexAdapter adapter(QdrantVectorOperations operations) {
        return new QdrantVectorIndexAdapter(
                properties(true, "qdrant", "localhost", 6334, "learning_os_chunks"),
                operations
        );
    }

    private RagVectorProperties properties(
            boolean enabled,
            String provider,
            String host,
            int port,
            String collection
    ) {
        return new RagVectorProperties(
                enabled,
                provider,
                new RagVectorProperties.Qdrant(host, port, false, "", collection, false, Duration.ofSeconds(2), 0)
        );
    }

    static class CapturingOperations implements QdrantVectorOperations {
        private final AtomicReference<QdrantVectorUpsertCommand> upsertCommand = new AtomicReference<>();
        private final AtomicReference<QdrantVectorSearchCommand> searchCommand = new AtomicReference<>();
        private boolean fail;
        private List<QdrantVectorSearchHit> searchResult = List.of();

        @Override
        public void deleteDocument(QdrantVectorDeleteCommand command) {
            if (fail) {
                throw new IllegalStateException("raw provider sk-live-secret delete failed");
            }
        }

        @Override
        public void upsert(QdrantVectorUpsertCommand command) {
            if (fail) {
                throw new IllegalStateException("raw provider sk-live-secret raw chunk");
            }
            upsertCommand.set(command);
        }

        @Override
        public List<QdrantVectorSearchHit> search(QdrantVectorSearchCommand command) {
            if (fail) {
                throw new IllegalStateException("raw provider sk-live-secret search failed");
            }
            searchCommand.set(command);
            return searchResult;
        }
    }
}
