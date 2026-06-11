package com.learningos.rag.application;

import com.learningos.rag.domain.KbDocChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RrfRankerTest {

    @Test
    void fusesRankedBranchesByRrfScoreAndDeduplicatesChunks() {
        KbDocChunk a = chunk("chunk_a");
        KbDocChunk b = chunk("chunk_b");
        KbDocChunk c = chunk("chunk_c");

        List<KbDocChunk> fused = new RrfRanker().fuse(
                List.of(
                        List.of(a, b, c),
                        List.of(b, c, a)
                ),
                3,
                60
        );

        assertThat(fused)
                .extracting(KbDocChunk::getId)
                .containsExactly("chunk_b", "chunk_a", "chunk_c");
    }

    @Test
    void appliesTopKAfterFusion() {
        KbDocChunk a = chunk("chunk_a");
        KbDocChunk b = chunk("chunk_b");
        KbDocChunk c = chunk("chunk_c");

        List<KbDocChunk> fused = new RrfRanker().fuse(
                List.of(
                        List.of(a, b, c),
                        List.of(b, c, a)
                ),
                1,
                60
        );

        assertThat(fused)
                .extracting(KbDocChunk::getId)
                .containsExactly("chunk_b");
    }

    private KbDocChunk chunk(String id) {
        KbDocChunk chunk = new KbDocChunk();
        chunk.setId(id);
        chunk.setKbId("kb_sql");
        chunk.setDocumentId("doc_" + id);
        chunk.setDocumentVersion(1);
        chunk.setChunkIndex(0);
        chunk.setContent(id + " content");
        chunk.setSectionTitle(id);
        return chunk;
    }
}
