package com.learningos.rag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.repository.KbDocChunkRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PocContextBuilderTest {

    private final KbDocChunkRepository repository = mock(KbDocChunkRepository.class);
    private final PocContextBuilder builder = new PocContextBuilder(repository, new ObjectMapper());

    @Test
    void expandsSourceChunkWithParentAdjacentAndChildContextWithinAllowedKnowledgeBase() {
        KbDocChunk parent = chunk(
                "chunk_parent",
                "kb_allowed",
                "doc_sql",
                1,
                0,
                "Transactions keep commit and rollback context together.",
                "Transactions",
                headingMetadata("Transactions")
        );
        KbDocChunk own = chunk(
                "chunk_isolation",
                "kb_allowed",
                "doc_sql",
                1,
                1,
                "Isolation explains visibility rules between concurrent transactions.",
                "Isolation",
                headingMetadata("Transactions", "Isolation")
        );
        KbDocChunk child = chunk(
                "chunk_phantom",
                "kb_allowed",
                "doc_sql",
                1,
                2,
                "Phantom reads appear when range queries observe new rows.",
                "Phantom reads",
                headingMetadata("Transactions", "Isolation", "Phantom reads")
        );
        KbDocChunk forbidden = chunk(
                "chunk_forbidden",
                "kb_hidden",
                "doc_sql",
                1,
                3,
                "Forbidden course material must never enter context.",
                "Hidden",
                headingMetadata("Transactions", "Isolation", "Hidden")
        );
        when(repository.findByDocumentIdOrderByChunkIndex("doc_sql"))
                .thenReturn(List.of(parent, own, child, forbidden));

        PocContextResult result = builder.build(List.of("kb_allowed"), List.of(own));

        assertThat(result.contextChunks())
                .extracting(KbDocChunk::getId)
                .containsExactly("chunk_parent", "chunk_isolation", "chunk_phantom");
        assertThat(result.expandedChunkCount()).isEqualTo(2);
        assertThat(result.parentChunkCount()).isEqualTo(1);
        assertThat(result.childChunkCount()).isEqualTo(1);
        assertThat(result.adjacentChunkCount()).isEqualTo(2);
        assertThat(result.toMetadata())
                .containsEntry("enabled", true)
                .containsEntry("sourceChunkCount", 1)
                .containsEntry("contextChunkCount", 3)
                .containsEntry("expandedChunkCount", 2);
        assertThat(result.toMetadata().toString())
                .doesNotContain("Forbidden course material", "Phantom reads appear", "Isolation explains");
    }

    @Test
    void doesNotExpandAcrossDocumentVersions() {
        KbDocChunk oldVersionNeighbor = chunk(
                "chunk_old",
                "kb_allowed",
                "doc_sql",
                1,
                0,
                "Old version context must not be mixed.",
                "Old",
                headingMetadata("Old")
        );
        KbDocChunk source = chunk(
                "chunk_current",
                "kb_allowed",
                "doc_sql",
                2,
                1,
                "Current version source chunk.",
                "Current",
                headingMetadata("Current")
        );
        when(repository.findByDocumentIdOrderByChunkIndex("doc_sql"))
                .thenReturn(List.of(oldVersionNeighbor, source));

        PocContextResult result = builder.build(List.of("kb_allowed"), List.of(source));

        assertThat(result.contextChunks())
                .extracting(KbDocChunk::getId)
                .containsExactly("chunk_current");
        assertThat(result.expandedChunkCount()).isZero();
    }

    private KbDocChunk chunk(
            String id,
            String kbId,
            String documentId,
            int documentVersion,
            int chunkIndex,
            String content,
            String sectionTitle,
            String metadataJson
    ) {
        KbDocChunk chunk = new KbDocChunk();
        chunk.setId(id);
        chunk.setKbId(kbId);
        chunk.setDocumentId(documentId);
        chunk.setDocumentVersion(documentVersion);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setSectionTitle(sectionTitle);
        chunk.setMetadataJson(metadataJson);
        chunk.setCreatedAt(Instant.now());
        return chunk;
    }

    private String headingMetadata(String... headings) {
        return new ObjectMapper().valueToTree(Map.of("headingPath", List.of(headings))).toString();
    }
}
