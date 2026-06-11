package com.learningos.rag.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoopVectorIndexAdapterTest {

    private final NoopVectorIndexAdapter adapter = new NoopVectorIndexAdapter();

    @Test
    void staysDisabledAndReturnsSafeResults() {
        assertThat(adapter.isEnabled()).isFalse();
        assertThat(adapter.deleteDocument("kb_1", "doc_1", 1))
                .satisfies(result -> {
                    assertThat(result.status()).isEqualTo(VectorIndexStatus.DISABLED);
                    assertThat(result.errorCode()).isNull();
                });
        assertThat(adapter.upsert(new VectorUpsertRequest(
                "kb_1",
                "doc_1",
                1,
                List.of(new VectorChunkReference("chunk_1", "hash_1", 0))
        )))
                .satisfies(result -> {
                    assertThat(result.status()).isEqualTo(VectorIndexStatus.DISABLED);
                    assertThat(result.errorCode()).isNull();
                });
        assertThat(adapter.search(new VectorSearchRequest(
                List.of("kb_1"),
                5,
                new EmbeddingVector("query", new float[]{0.1f, 0.2f})
        )))
                .satisfies(result -> {
                    assertThat(result.status()).isEqualTo(VectorIndexStatus.DISABLED);
                    assertThat(result.hits()).isEmpty();
                    assertThat(result.errorCode()).isNull();
                });
    }

    @Test
    void vectorUpsertRequestDoesNotExposeRawChunkContent() {
        VectorUpsertRequest request = new VectorUpsertRequest(
                "kb_1",
                "doc_1",
                1,
                List.of(new VectorChunkReference(
                        "chunk_1",
                        "hash_1",
                        0,
                        new EmbeddingVector("chunk_1", new float[]{0.1f, 0.2f, 0.3f})
                ))
        );

        assertThat(request.toString())
                .doesNotContain("raw chunk", "SQL JOIN duplicates", "apiKey", "secret", "content=", "0.1", "0.2", "0.3");
    }
}
