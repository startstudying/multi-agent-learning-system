package com.learningos.rag.repository;

import com.learningos.rag.domain.KbDocChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface KbDocChunkRepository extends JpaRepository<KbDocChunk, String> {

    List<KbDocChunk> findTop20ByKbIdInOrderByCreatedAtDesc(Collection<String> allowedKbIds);

    List<KbDocChunk> findByDocumentIdOrderByChunkIndex(String documentId);

    @Modifying
    @Query("delete from KbDocChunk chunk where chunk.documentId = :documentId")
    int deleteByDocumentId(String documentId);
}
